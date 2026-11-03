package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SocialLoginRequest(
        @NotBlank @Size(max = 4096) String idToken
) {
}
