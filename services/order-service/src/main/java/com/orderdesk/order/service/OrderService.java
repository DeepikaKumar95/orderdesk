package com.orderdesk.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderdesk.order.api.dto.CreateOrderRequest;
import com.orderdesk.order.api.dto.OrderResponse;
import com.orderdesk.order.api.dto.OrderSummary;
import com.orderdesk.order.domain.*;
import com.orderdesk.order.events.OrderEvent;
import com.orderdesk.order.outbox.OutboxEvent;
import com.orderdesk.order.outbox.OutboxRepository;
import com.orderdesk.order.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use-case layer. Owns the transaction boundary. The order row, the outbox row and the
 * idempotency row are committed atomically - there is no dual write to Kafka here.
 */
@Service
public class OrderService {
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final IdempotencyKeyRepository idempotency;
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    private final MeterRegistry metrics;

    public OrderService(OrderRepository orders, CustomerRepository customers, IdempotencyKeyRepository idempotency,
                        OutboxRepository outbox, ObjectMapper json, MeterRegistry metrics) {
        this.orders = orders; this.customers = customers; this.idempotency = idempotency;
        this.outbox = outbox; this.json = json; this.metrics = metrics;
    }

    @Transactional
    public OrderResponse place(CreateOrderRequest req, String idempotencyKey) {
        var existing = idempotency.findById(idempotencyKey);
        if (existing.isPresent()) {
            return get(existing.get().getOrderId());          // safe retry: same response, no second order
        }
        Customer customer = customers.findById(req.customerId())
                .orElseThrow(() -> new IllegalStateException("Unknown customer " + req.customerId()));

        Order order = Order.create(customer, req.lines().stream()
                .map(l -> new Order.NewLine(l.sku(), l.qty(), l.unitPrice())).toList());
        orders.save(order);

        var event = new OrderEvent.OrderPlaced(UUID.randomUUID(), order.getId(), order.getCustomerId(), order.getTotal(),
                order.getLines().stream().map(l -> new OrderEvent.Line(l.getSku(), l.getQty())).toList(), Instant.now());
        outbox.save(OutboxEvent.of(event.eventId(), "Order", order.getId().toString(), "OrderPlaced", toJson(event)));
        idempotency.save(new IdempotencyKey(idempotencyKey, order.getId()));

        metrics.counter("orders_placed_total").increment();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return orders.findWithLinesById(id).map(OrderResponse::from).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> search(String customerId, Pageable page) {
        return orders.search(customerId, page);
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        Order order = orders.findWithLinesById(id).orElseThrow(() -> new OrderNotFoundException(id));
        if (order.getStatus() == OrderStatus.SHIPPED) throw new IllegalStateException("Shipped orders cannot be cancelled");
        order.transition(OrderStatus.CANCELLED);
        var event = new OrderEvent.OrderCancelled(UUID.randomUUID(), order.getId(), Instant.now());
        outbox.save(OutboxEvent.of(event.eventId(), "Order", order.getId().toString(), "OrderCancelled", toJson(event)));
        return OrderResponse.from(order);
    }

    /** Called by the inventory-reply consumer (see InventoryReplyListener) to move PENDING -> RESERVED/REJECTED. */
    @Transactional
    public void applyInventoryResult(UUID orderId, boolean reserved) {
        orders.findById(orderId).ifPresent(o -> {
            if (o.getStatus() == OrderStatus.PENDING) o.transition(reserved ? OrderStatus.RESERVED : OrderStatus.REJECTED);
        });
    }

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize event", e); }
    }
}
