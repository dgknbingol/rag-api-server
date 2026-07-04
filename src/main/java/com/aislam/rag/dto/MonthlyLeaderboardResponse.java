package com.aislam.rag.dto;

import java.util.List;
import java.util.UUID;

public record MonthlyLeaderboardResponse(
        List<LeaderboardEntryResponse> entries,
        UUID currentPlayerId,
        LeaderboardEntryResponse currentPlayer,
        int currentRank,
        long totalPlayers
) {
}
