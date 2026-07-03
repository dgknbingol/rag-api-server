package com.aislam.rag.dto;

import java.util.UUID;

public record QuizAnswerCheckRequest(UUID optionId, Long responseTimeMs) {
}
