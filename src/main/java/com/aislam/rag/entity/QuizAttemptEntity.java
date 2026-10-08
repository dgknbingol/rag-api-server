package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "quiz_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "event_id"})
)
public class QuizAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private QuizPlayerEntity player;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int questionCount;

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean prizeEligibleAtJoin = true;

    @Column(nullable = false, updatable = false)
    private Instant completedAt;

    protected QuizAttemptEntity() {
    }

    public QuizAttemptEntity(
            QuizPlayerEntity player,
            String eventId,
            int score,
            int correctCount,
            int questionCount,
            boolean prizeEligibleAtJoin
    ) {
        this.player = player;
        this.eventId = eventId;
        this.score = score;
        this.correctCount = correctCount;
        this.questionCount = questionCount;
        this.prizeEligibleAtJoin = prizeEligibleAtJoin;
    }

    @PrePersist
    void onCreate() {
        if (completedAt == null) {
            completedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public QuizPlayerEntity getPlayer() {
        return player;
    }

    public String getEventId() {
        return eventId;
    }

    public int getScore() {
        return score;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public boolean isPrizeEligibleAtJoin() {
        return prizeEligibleAtJoin;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
