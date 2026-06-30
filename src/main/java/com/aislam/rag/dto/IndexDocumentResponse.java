package com.aislam.rag.dto;

import java.util.List;

public record IndexDocumentResponse(
        String documentId,
        int chunkCount,
        String status,
        SectionTitleIndexStats sectionTitleStats
) {
    public static IndexDocumentResponse indexed(String documentId, int chunkCount) {
        return indexed(documentId, chunkCount, null);
    }

    public static IndexDocumentResponse indexed(String documentId, int chunkCount, SectionTitleIndexStats sectionTitleStats) {
        return new IndexDocumentResponse(documentId, chunkCount, "INDEXED", sectionTitleStats);
    }

    public static IndexDocumentResponse empty(String documentId) {
        return new IndexDocumentResponse(documentId, 0, "NO_CONTENT", SectionTitleIndexStats.fromChunks(List.of()));
    }
}
