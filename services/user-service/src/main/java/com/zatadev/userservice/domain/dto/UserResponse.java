package com.zatadev.userservice.domain.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String username,
        String email,
        String role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}