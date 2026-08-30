package com.orderdesk.inventory.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class Reservation {
    public enum State { RESERVED, RELEASED, COMMITTED }
    @Id @Column(name = "reservation_id") private UUID id = UUID.randomUUID();
    @Column(name = "order_id") private UUID orderId;
    private String sku;
    private int qty;
    @Enumerated(EnumType.STRING) private State state;
    @Column(name = "created_at") @JdbcTypeCode(SqlTypes.TIMESTAMP) private Instant createdAt = Instant.now();
    protected Reservation() {}
    public Reservation(UUID orderId, String sku, int qty) { this.orderId = orderId; this.sku = sku; this.qty = qty; this.state = State.RESERVED; }
    public UUID getOrderId() { return orderId; }
    public String getSku() { return sku; }
    public int getQty() { return qty; }
    public State getState() { return state; }
    public void release() { this.state = State.RELEASED; }
}
