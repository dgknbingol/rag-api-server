package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUserEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private boolean premium;

    @Column
    private Instant premiumExpiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUserEntity() {
    }

    public AppUserEntity(UUID id) {
        this.id = id;
        this.premium = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public boolean isPremium() {
        return premium;
    }

    public Instant getPremiumExpiresAt() {
        return premiumExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public void setPremiumExpiresAt(Instant premiumExpiresAt) {
        this.premiumExpiresAt = premiumExpiresAt;
    }
}
