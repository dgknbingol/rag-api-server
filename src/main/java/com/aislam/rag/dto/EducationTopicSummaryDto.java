package com.aislam.rag.dto;

public record EducationTopicSummaryDto(
        String id,
        String title,
        String summary,
        boolean hasContent
) {
}
