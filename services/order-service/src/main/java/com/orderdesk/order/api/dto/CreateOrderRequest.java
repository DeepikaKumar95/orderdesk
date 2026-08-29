package com.orderdesk.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotEmpty List<@Valid LineItem> lines) {

    public record LineItem(@NotBlank String sku,
                           @Positive int qty,
                           @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {}
}
