package com.aislam.rag.repository;

import com.aislam.rag.entity.ChatDailyUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ChatDailyUsageRepository extends JpaRepository<ChatDailyUsageEntity, UUID> {

    Optional<ChatDailyUsageEntity> findByAppUserIdAndUsageDate(UUID appUserId, LocalDate usageDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatDailyUsageEntity u
            SET u.count = u.count + 1, u.updatedAt = :now
            WHERE u.appUserId = :appUserId
              AND u.usageDate = :usageDate
              AND u.count < :limit
            """)
    int tryIncrement(
            @Param("appUserId") UUID appUserId,
            @Param("usageDate") LocalDate usageDate,
            @Param("limit") int limit,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatDailyUsageEntity u
            SET u.count = u.count - 1, u.updatedAt = :now
            WHERE u.appUserId = :appUserId
              AND u.usageDate = :usageDate
              AND u.count > 0
            """)
    int tryDecrement(
            @Param("appUserId") UUID appUserId,
            @Param("usageDate") LocalDate usageDate,
            @Param("now") Instant now
    );
}
