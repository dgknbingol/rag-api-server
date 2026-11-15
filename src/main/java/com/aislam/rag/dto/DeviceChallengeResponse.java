package com.aislam.rag.dto;

public record DeviceChallengeResponse(
        String challenge,
        long expiresInSeconds
) {
}
