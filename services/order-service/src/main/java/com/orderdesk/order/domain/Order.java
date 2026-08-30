package com.orderdesk.order.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root. Business rules live here (or in OrderService), never in the controller.
 * The credit-limit rule is ported verbatim from legacy OrderDesk.Core.OrderService so the
 * .NET characterization tests remain the contract.
 */
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "order_id")
    private UUID id;
    @Column(name = "customer_id")
    private String customerId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal total;
    @Version
    private long version;
    @Column(name = "created_at", updatable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;
    @Column(name = "updated_at")
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @OrderBy("lineNo")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {}

    public record NewLine(String sku, int qty, BigDecimal unitPrice) {}

    public static Order create(Customer customer, List<NewLine> newLines) {
        Order o = new Order();
        o.id = UUID.randomUUID();
        o.customerId = customer.getId();
        o.status = OrderStatus.PENDING;
        o.createdAt = Instant.now();
        o.updatedAt = o.createdAt;
        int n = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (NewLine l : newLines) {
            OrderLine line = new OrderLine(o.id, n++, l.sku(), l.qty(), l.unitPrice());
            o.lines.add(line);
            total = total.add(line.lineTotal());
        }
        o.total = total;
        if (total.compareTo(customer.getCreditLimit()) > 0) {
            throw new InsufficientCreditException(customer.getId());
        }
        return o;
    }

    public void transition(OrderStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return lines; }
}
