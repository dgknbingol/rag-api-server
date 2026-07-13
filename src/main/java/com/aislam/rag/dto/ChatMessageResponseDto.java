package com.aislam.rag.dto;

import java.util.UUID;

public record ChatMessageResponseDto(
        ChatMessageDto userMessage,
        ChatMessageDto assistantMessage,
        UUID conversationId
) {
}
