package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record UnregisterPushRequest(
        @NotBlank String deviceId
) {
}
