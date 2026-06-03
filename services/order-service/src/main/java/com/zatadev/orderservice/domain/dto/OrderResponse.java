package com.zatadev.orderservice.domain.dto;

import com.zatadev.orderservice.domain.entity.Order;
import com.zatadev.orderservice.domain.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record OrderResponse(
        UUID id,
        UUID customerId,
        UUID productId,
        Integer quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}