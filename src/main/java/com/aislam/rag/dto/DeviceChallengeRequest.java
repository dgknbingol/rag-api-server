package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceChallengeRequest(
        @NotBlank String deviceId
) {
}
