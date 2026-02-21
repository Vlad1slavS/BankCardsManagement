package com.example.bankcards.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, char[]> {

    @Override
    public boolean isValid(char[] password, ConstraintValidatorContext context) {
        if (password == null || password.length < 8) {
            buildMessage(context, "Пароль должен содержать минимум 8 символов");
            return false;
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper) {
            buildMessage(context, "Пароль должен содержать минимум одну заглавную букву");
            return false;
        }
        if (!hasLower) {
            buildMessage(context, "Пароль должен содержать минимум одну строчную букву");
            return false;
        }
        if (!hasDigit) {
            buildMessage(context, "Пароль должен содержать минимум одну цифру");
            return false;
        }

        return true;
    }

    private void buildMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
