package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostChatMessageRequest(
        @NotBlank(message = "content must not be blank")
        @Size(max = 2000, message = "content must be at most 2000 characters")
        String content
) {
}
