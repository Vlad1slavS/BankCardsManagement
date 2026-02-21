package com.example.bankcards.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TransferResponse(
        UUID id,
        String fromCardMasked,
        String toCardMasked,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
