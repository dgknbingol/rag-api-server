package com.aislam.rag.auth;

public record SocialIdentity(
        String provider,
        String subject,
        String email,
        String displayName
) {
}
