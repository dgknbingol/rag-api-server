package com.aislam.rag.dto;

public record PdfUploadResponse(
        String documentId,
        String title,
        int chunkCount,
        String status,
        SectionTitleIndexStats sectionTitleStats
) {
    public static PdfUploadResponse indexed(
            String documentId,
            String title,
            int chunkCount,
            SectionTitleIndexStats sectionTitleStats
    ) {
        return new PdfUploadResponse(documentId, title, chunkCount, "INDEXED", sectionTitleStats);
    }

    public static PdfUploadResponse empty(String documentId, String title) {
        return new PdfUploadResponse(
                documentId,
                title,
                0,
                "NO_CONTENT",
                new SectionTitleIndexStats(0, 0, 0)
        );
    }
}
