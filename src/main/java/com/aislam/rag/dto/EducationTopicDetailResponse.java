package com.aislam.rag.dto;

public record EducationTopicDetailResponse(
    String id,
    String categoryId,
    String categoryTitle,
    String moduleId,
    String moduleTitle,
    String title,
    String summary,
    EducationTopicContentDto content) {}
