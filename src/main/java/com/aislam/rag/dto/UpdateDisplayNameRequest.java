package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
        @NotBlank @Size(min = 2, max = 80) String displayName
) {
}

