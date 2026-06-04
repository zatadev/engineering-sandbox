package com.zatadev.notificationservice.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mirror of order-service OrderCreatedEvent.
 * Intentionally duplicated — no shared dependency before common module (ZAT-174).
 */
public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        UUID productId,
        Integer quantity,
        BigDecimal totalPrice,
        Instant occurredAt
) {}
