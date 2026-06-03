package com.zatadev.orderservice.domain.dto;

import com.zatadev.orderservice.domain.entity.OrderStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CancelOrderResponse(
        UUID id,
        OrderStatus status,
        Instant updatedAt,
        String message
) {}