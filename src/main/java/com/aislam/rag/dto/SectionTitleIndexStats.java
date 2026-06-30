package com.aislam.rag.dto;

import com.aislam.rag.domain.TextChunk;

import java.util.List;

public record SectionTitleIndexStats(
        int totalChunks,
        int chunksWithSectionTitle,
        int chunksWithoutSectionTitle
) {
    public static SectionTitleIndexStats fromChunks(List<TextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new SectionTitleIndexStats(0, 0, 0);
        }

        int withSectionTitle = 0;
        for (TextChunk chunk : chunks) {
            if (hasSectionTitle(chunk)) {
                withSectionTitle++;
            }
        }

        int total = chunks.size();
        return new SectionTitleIndexStats(total, withSectionTitle, total - withSectionTitle);
    }

    private static boolean hasSectionTitle(TextChunk chunk) {
        return chunk.sectionTitle() != null && !chunk.sectionTitle().isBlank();
    }
}
