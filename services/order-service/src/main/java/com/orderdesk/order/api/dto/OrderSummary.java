package com.orderdesk.order.api.dto;

import com.orderdesk.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** JPQL projection - the list endpoint never materializes entities. */
public record OrderSummary(UUID id, String customerId, OrderStatus status, BigDecimal total, Instant createdAt) {}
