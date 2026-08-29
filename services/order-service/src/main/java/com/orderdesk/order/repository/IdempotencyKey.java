package com.orderdesk.order.repository;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id @Column(name = "idem_key") private String key;
    @Column(name = "order_id") private UUID orderId;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    protected IdempotencyKey() {}
    public IdempotencyKey(String key, UUID orderId) { this.key = key; this.orderId = orderId; }
    public UUID getOrderId() { return orderId; }
}
