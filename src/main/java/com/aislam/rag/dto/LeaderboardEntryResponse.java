package com.aislam.rag.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        UUID id,
        String name,
        long score,
        String initials,
        String accent
) {
}
