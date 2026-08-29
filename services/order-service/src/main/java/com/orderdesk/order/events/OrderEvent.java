package com.orderdesk.order.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Event contract published on orders.events. Java 17 sealed hierarchy; JSON with a type discriminator. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderEvent.OrderPlaced.class, name = "OrderPlaced"),
    @JsonSubTypes.Type(value = OrderEvent.OrderCancelled.class, name = "OrderCancelled")
})
public sealed interface OrderEvent permits OrderEvent.OrderPlaced, OrderEvent.OrderCancelled {
    UUID eventId();
    UUID orderId();
    Instant occurredAt();

    record Line(String sku, int qty) {}

    record OrderPlaced(UUID eventId, UUID orderId, String customerId, BigDecimal total,
                       List<Line> lines, Instant occurredAt) implements OrderEvent {}

    record OrderCancelled(UUID eventId, UUID orderId, Instant occurredAt) implements OrderEvent {}
}
