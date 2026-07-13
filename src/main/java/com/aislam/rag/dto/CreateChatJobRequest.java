package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateChatJobRequest(
        @NotBlank(message = "question must not be blank")
        @Size(max = 2000, message = "question must be at most 2000 characters")
        String question,
        UUID conversationId
) {
}
