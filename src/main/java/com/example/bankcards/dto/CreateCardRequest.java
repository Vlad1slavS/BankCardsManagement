package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCardRequest(

        @NotNull(message = "ID владельца обязателен")
        UUID ownerId,

        @NotBlank(message = "Номер карты обязателен")
        @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать ровно 16 цифр (без пробелов)")
        String cardNumber,

        @NotBlank(message = "Имя держателя карты обязательно")
        @Size(max = 100, message = "Имя держателя не должно превышать 100 символов")
        String holderName,

        @NotNull(message = "Дата окончания действия обязательна")
        @Future(message = "Дата окончания действия должна быть в будущем")
        LocalDate expiryDate,

        @NotNull(message = "Начальный баланс обязателен")
        @DecimalMin(value = "0.00", message = "Баланс не может быть отрицательным")
        BigDecimal initialBalance

) {
}
