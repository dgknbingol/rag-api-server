package com.aislam.rag.dto;

public record ApiErrorResponse(
        String message,
        String code
) {
}
