package com.aislam.rag.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatConversationSummaryDto(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
