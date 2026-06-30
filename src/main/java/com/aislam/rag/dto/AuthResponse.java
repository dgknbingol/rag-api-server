package com.aislam.rag.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserProfileResponse user
) {
}
