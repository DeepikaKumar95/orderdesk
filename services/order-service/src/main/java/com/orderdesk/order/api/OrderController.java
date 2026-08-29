package com.orderdesk.order.api;

import com.orderdesk.order.api.dto.CreateOrderRequest;
import com.orderdesk.order.api.dto.OrderResponse;
import com.orderdesk.order.api.dto.OrderSummary;
import com.orderdesk.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/** HTTP concerns only: routing, validation, status codes. No business rules here. */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req,
                                                @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        OrderResponse created = orders.place(req, Optional.ofNullable(key).orElse(UUID.randomUUID().toString()));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) { return orders.get(id); }

    @GetMapping
    public Page<OrderSummary> list(@RequestParam(required = false) String customerId,
                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
        return orders.search(customerId, page);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) { return orders.cancel(id); }
}
