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
        name = "quiz_participations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "event_id"})
)
public class QuizParticipationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private QuizPlayerEntity player;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false)
    private UUID appUserId;

    @Column(nullable = false)
    private boolean prizeEligibleAtJoin;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected QuizParticipationEntity() {
    }

    public QuizParticipationEntity(
            QuizPlayerEntity player,
            String eventId,
            UUID appUserId,
            boolean prizeEligibleAtJoin
    ) {
        this.player = player;
        this.eventId = eventId;
        this.appUserId = appUserId;
        this.prizeEligibleAtJoin = prizeEligibleAtJoin;
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
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

    public UUID getAppUserId() {
        return appUserId;
    }

    public boolean isPrizeEligibleAtJoin() {
        return prizeEligibleAtJoin;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
