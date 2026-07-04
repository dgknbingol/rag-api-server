package com.aislam.rag.dto;

import java.time.Instant;

public record ChatQuotaResponse(
        int limit,
        int used,
        int remaining,
        boolean premium,
        Instant resetsAt
) {
}
