package com.aislam.rag.dto;

import com.aislam.rag.entity.UserEntity;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String email,
        String displayName,
        boolean premium,
        Instant premiumExpiresAt,
        Instant createdAt
) {
    public static UserProfileResponse from(UserEntity user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.isPremium(),
                user.getPremiumExpiresAt(),
                user.getCreatedAt()
        );
    }
}
