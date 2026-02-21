package com.example.bankcards.dto;

import com.example.bankcards.validator.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
        String username,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Неверный формат email")
        String email,

        @ValidPassword
        char[] password
) {
}
