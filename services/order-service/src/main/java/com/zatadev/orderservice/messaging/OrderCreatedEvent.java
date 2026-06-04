package com.zatadev.orderservice.messaging;

import com.zatadev.orderservice.domain.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        UUID productId,
        Integer quantity,
        BigDecimal totalPrice,
        Instant occurredAt
) {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                Instant.now()
        );
    }
}