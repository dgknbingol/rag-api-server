package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;

/** QA: anında test push gönder. */
public record TestPushRequest(
        @NotBlank String deviceId,
        String title,
        String body
) {
}
