package com.example.bankcards.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String username,
        String role
) {
}
