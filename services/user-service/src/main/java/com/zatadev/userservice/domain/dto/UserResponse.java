package com.zatadev.userservice.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String username,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}