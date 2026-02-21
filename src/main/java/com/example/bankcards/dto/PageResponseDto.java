package com.example.bankcards.dto;

import java.util.List;

public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
