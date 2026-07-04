package com.aislam.rag.repository;

import com.aislam.rag.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, UUID> {

    Optional<QuizAttemptEntity> findByPlayerIdAndEventId(UUID playerId, String eventId);

    List<QuizAttemptEntity> findByPlayerIdOrderByCompletedAtDesc(UUID playerId);

    @Query("""
            SELECT a.player.id AS playerId,
                   a.player.displayName AS displayName,
                   SUM(a.score) AS totalScore,
                   COUNT(a) AS quizzesCompleted,
                   MAX(a.score) AS bestDailyScore
            FROM QuizAttemptEntity a
            WHERE a.completedAt >= :startInclusive AND a.completedAt < :endExclusive
            GROUP BY a.player.id, a.player.displayName
            ORDER BY totalScore DESC
            """)
    List<MonthlyAggregateRow> findMonthlyAggregates(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query("""
            SELECT COUNT(DISTINCT a.player.id)
            FROM QuizAttemptEntity a
            WHERE a.completedAt >= :startInclusive AND a.completedAt < :endExclusive
            """)
    long countDistinctPlayersInRange(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    interface MonthlyAggregateRow {
        UUID getPlayerId();

        String getDisplayName();

        long getTotalScore();

        long getQuizzesCompleted();

        int getBestDailyScore();
    }
}
