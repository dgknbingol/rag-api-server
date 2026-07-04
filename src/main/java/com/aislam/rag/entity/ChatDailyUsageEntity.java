package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "chat_daily_usage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"app_user_id", "usage_date"})
)
public class ChatDailyUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "app_user_id", nullable = false)
    private UUID appUserId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private int count;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ChatDailyUsageEntity() {
    }

    public ChatDailyUsageEntity(UUID appUserId, LocalDate usageDate) {
        this.appUserId = appUserId;
        this.usageDate = usageDate;
        this.count = 0;
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

    public UUID getAppUserId() {
        return appUserId;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public int getCount() {
        return count;
    }

    public void incrementCount() {
        this.count += 1;
    }
}
