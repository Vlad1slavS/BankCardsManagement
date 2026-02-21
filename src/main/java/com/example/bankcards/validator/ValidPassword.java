package com.example.bankcards.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для валидации пароля
 * Пароль должен содержать минимум 8 символов, включая:
 * хотя бы одну заглавную букву, одну строчную букву и одну цифру.
 *
 * @see PasswordValidator
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
    String message() default "Неверный формат пароля";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
