package com.zatadev.orderservice.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CreateOrderRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "totalPrice is required")
        @DecimalMin(value = "0.01", message = "totalPrice must be greater than 0")
        BigDecimal totalPrice
) {}
