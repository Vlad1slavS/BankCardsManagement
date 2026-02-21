package com.example.bankcards.dto;

import com.example.bankcards.enums.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt
) {
}

