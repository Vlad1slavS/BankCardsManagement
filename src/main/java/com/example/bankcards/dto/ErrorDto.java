package com.example.bankcards.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorDto(
        String message,
        LocalDateTime timestamp,
        String path) {
}
