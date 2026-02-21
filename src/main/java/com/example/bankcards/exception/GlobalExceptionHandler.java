package com.example.bankcards.exception;

import com.example.bankcards.dto.ErrorDto;
import com.example.bankcards.enums.CardStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDto.builder()
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorDto> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.builder()
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }


    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorDto> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDto.builder()
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(CardOperationException.class)
    public ResponseEntity<ErrorDto> handleCardOperation(CardOperationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.builder()
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDto.builder()
                        .message("Неверные учетные данные")
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationExceptions(MethodArgumentNotValidException ex,
                                                               HttpServletRequest req) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto.builder()
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .path(req.getRequestURI())
                .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest req) {
        String errorMessage = "Неверный формат входных данных";

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            Class<?> targetType = invalidFormatEx.getTargetType();
            Object value = invalidFormatEx.getValue();

            errorMessage = String.format(
                    "Неверное значение '%s' для типа '%s' ",
                    value,
                    targetType != null ? targetType.getSimpleName() : "unknown target type");
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.builder()
                        .message(errorMessage)
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        String errorMessage = "Неверный формат входных данных";

        if (ex.getRequiredType() == CardStatus.class) {
            Object value = ex.getValue();
            String allowedValues = Arrays.stream(CardStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            errorMessage = String.format(
                    "Неверное значение '%s'. Допустимые значения: %s",
                    value,
                    allowedValues
            );
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.builder()
                        .message(errorMessage)
                        .timestamp(LocalDateTime.now())
                        .path(req.getRequestURI())
                        .build());
    }

}
