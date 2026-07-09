package com.aislam.rag.dto;

public record EducationQuizSeedDto(
    String id,
    String title,
    int questionCount,
    int passPercent,
    String achievementId) {}
