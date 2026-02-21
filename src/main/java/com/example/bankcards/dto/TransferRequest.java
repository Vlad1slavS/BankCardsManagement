package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "ID карты отправителя обязателен")
        UUID fromCardId,

        @NotNull(message = "ID карты получателя обязателен")
        UUID toCardId,

        @NotNull(message = "Сумма перевода обязательна")
        @DecimalMin(value = "0.01", message = "Сумма перевода должна быть больше 0")
        BigDecimal amount
) {
}