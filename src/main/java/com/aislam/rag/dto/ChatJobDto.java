package com.aislam.rag.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatJobDto(
        UUID id,
        UUID conversationId,
        String status,
        String question,
        String answer,
        String errorMessage,
        String errorCode,
        Instant createdAt,
        Instant finishedAt
) {
}
