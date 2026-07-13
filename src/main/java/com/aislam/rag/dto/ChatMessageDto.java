package com.aislam.rag.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        String role,
        String content,
        Instant createdAt
) {
}
