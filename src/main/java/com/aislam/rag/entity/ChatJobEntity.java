package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chat_jobs",
        indexes = {
                @Index(name = "idx_chat_jobs_status_created", columnList = "status, created_at")
        }
)
public class ChatJobEntity {

    public enum Status {
        PENDING,
        RUNNING,
        DONE,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "app_user_id", nullable = false)
    private UUID appUserId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 64)
    private String errorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant lockedAt;

    @Column
    private Instant finishedAt;

    protected ChatJobEntity() {
    }

    public ChatJobEntity(UUID appUserId, UUID conversationId, String question) {
        this.appUserId = appUserId;
        this.conversationId = conversationId;
        this.question = question;
        this.status = Status.PENDING;
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

    public UUID getConversationId() {
        return conversationId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void markRunning() {
        this.status = Status.RUNNING;
        this.lockedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markDone(String answer) {
        this.status = Status.DONE;
        this.answer = answer;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed(String message, String code) {
        this.status = Status.FAILED;
        this.errorMessage = message;
        this.errorCode = code;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
