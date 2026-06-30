package com.aislam.rag.domain;

public record SourceChunk(
        String id,
        String documentId,
        String title,
        String source,
        String sectionTitle,
        String content,
        Integer pageNumber,
        Integer chunkIndex,
        Double vectorScore,
        Double keywordScore,
        Double phraseScore,
        Double sectionTitleBoost,
        Double qualityPenalty,
        Double score
) {
    public static SourceChunk fromVectorSearch(
            String id,
            String documentId,
            String title,
            String source,
            String content,
            Integer pageNumber,
            Integer chunkIndex,
            double vectorScore
    ) {
        return fromVectorSearch(id, documentId, title, source, null, content, pageNumber, chunkIndex, vectorScore);
    }

    public static SourceChunk fromVectorSearch(
            String id,
            String documentId,
            String title,
            String source,
            String sectionTitle,
            String content,
            Integer pageNumber,
            Integer chunkIndex,
            double vectorScore
    ) {
        return new SourceChunk(
                id,
                documentId,
                title,
                source,
                sectionTitle,
                content,
                pageNumber,
                chunkIndex,
                vectorScore,
                0.0,
                0.0,
                0.0,
                0.0,
                vectorScore
        );
    }

    public SourceChunk withRerankScores(
            double keywordScore,
            double phraseScore,
            double sectionTitleBoost,
            double qualityPenalty,
            double finalScore
    ) {
        return new SourceChunk(
                id,
                documentId,
                title,
                source,
                sectionTitle,
                content,
                pageNumber,
                chunkIndex,
                vectorScore,
                keywordScore,
                phraseScore,
                sectionTitleBoost,
                qualityPenalty,
                finalScore
        );
    }

    public SourceChunk withContent(String content) {
        return new SourceChunk(
                id,
                documentId,
                title,
                source,
                sectionTitle,
                content,
                pageNumber,
                chunkIndex,
                vectorScore,
                keywordScore,
                phraseScore,
                sectionTitleBoost,
                qualityPenalty,
                score
        );
    }

    public SourceChunk withSectionTitle(String sectionTitle) {
        return new SourceChunk(
                id,
                documentId,
                title,
                source,
                sectionTitle,
                content,
                pageNumber,
                chunkIndex,
                vectorScore,
                keywordScore,
                phraseScore,
                sectionTitleBoost,
                qualityPenalty,
                score
        );
    }
}
