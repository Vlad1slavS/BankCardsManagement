package com.example.bankcards.dto;

import com.example.bankcards.enums.CardStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CardResponse(
        UUID id,
        String maskedNumber,
        String holderName,
        UUID ownerId,
        String ownerUsername,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

