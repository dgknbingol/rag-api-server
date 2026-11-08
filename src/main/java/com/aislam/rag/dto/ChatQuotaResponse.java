package com.aislam.rag.dto;

import java.time.Instant;

public record ChatQuotaResponse(
        int limit,
        int used,
        int remaining,
        boolean premium,
        Instant resetsAt,
        /** Bugün rewarded ile kazanılan ekstra soru hakkı. */
        int rewardedBonus,
        /** Bugün daha kaç rewarded izlenebilir (premium'da 0). */
        int rewardedRemaining
) {
}
