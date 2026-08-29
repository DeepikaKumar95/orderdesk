package com.orderdesk.order.api.dto;

import com.orderdesk.order.domain.Order;
import com.orderdesk.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, String customerId, OrderStatus status, BigDecimal total,
                            Instant createdAt, List<Line> lines) {
    public record Line(int lineNo, String sku, int qty, BigDecimal unitPrice) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getStatus(), o.getTotal(), o.getCreatedAt(),
                o.getLines().stream().map(l -> new Line(l.getLineNo(), l.getSku(), l.getQty(), l.getUnitPrice())).toList());
    }
}
