package com.aislam.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResultDto(
        String title,
        String source,
        String sectionTitle,
        String contentPreview,
        Integer pageNumber,
        Integer chunkIndex,
        Double finalScore,
        Double vectorScore,
        Double keywordScore,
        Double phraseScore,
        Double sectionTitleBoost,
        Double qualityPenalty
) {
}
