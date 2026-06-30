package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        String rerankerType,
        int initialCandidateCount,
        int finalSourceCount,
        int contentPreviewChars,
        int neighborWindow,
        int maxContextChars,
        int maxAnswerWords,
        int chunkSize,
        int chunkOverlap,
        int maxConcurrentChatRequests,
        int maxConcurrentEmbeddingRequests,
        long requestTimeoutSeconds,
        int maxSourceContentChars,
        int maxTotalSourcesChars,
        boolean relevanceGateEnabled,
        int relevanceGateTopChunks,
        double relevanceGateMinLexicalScore,
        double relevanceGateMinKeywordCoverage,
        double relevanceGateMinVectorScore
) {
    public static RagProperties forTests() {
        return new RagProperties(
                "keyword", 30, 5, 200, 1, 8000, 80, 1800, 250, 1, 4, 30, 1000, 3000,
                true, 5, 0.12, 0.7, 0.55
        );
    }
}
