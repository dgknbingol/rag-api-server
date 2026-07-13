package com.aislam.rag.repository;

import com.aislam.rag.entity.ChatJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatJobRepository extends JpaRepository<ChatJobEntity, UUID> {

    Optional<ChatJobEntity> findByIdAndAppUserId(UUID id, UUID appUserId);

    long countByStatus(ChatJobEntity.Status status);

    @Query(value = """
            SELECT id FROM chat_jobs
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> lockPendingIds(@Param("limit") int limit);
}
