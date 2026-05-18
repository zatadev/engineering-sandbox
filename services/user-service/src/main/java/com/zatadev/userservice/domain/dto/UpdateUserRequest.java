package com.zatadev.userservice.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record UpdateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email
) {}