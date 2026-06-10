package com.zatadev.order.contracts;

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
) {}
