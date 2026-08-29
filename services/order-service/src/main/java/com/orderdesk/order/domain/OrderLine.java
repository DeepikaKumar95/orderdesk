package com.orderdesk.order.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_lines")
@IdClass(OrderLine.Key.class)
public class OrderLine {
    @Id
    @Column(name = "order_id")
    private UUID orderId;
    @Id
    @Column(name = "line_no")
    private int lineNo;
    private String sku;
    private int qty;
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    protected OrderLine() {}

    OrderLine(UUID orderId, int lineNo, String sku, int qty, BigDecimal unitPrice) {
        this.orderId = orderId; this.lineNo = lineNo; this.sku = sku; this.qty = qty; this.unitPrice = unitPrice;
    }

    public int getLineNo() { return lineNo; }
    public String getSku() { return sku; }
    public int getQty() { return qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(qty)); }

    public static class Key implements Serializable {
        private UUID orderId;
        private int lineNo;
        public Key() {}
        public Key(UUID orderId, int lineNo) { this.orderId = orderId; this.lineNo = lineNo; }
        @Override public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(orderId, k.orderId) && lineNo == k.lineNo;
        }
        @Override public int hashCode() { return Objects.hash(orderId, lineNo); }
    }
}
