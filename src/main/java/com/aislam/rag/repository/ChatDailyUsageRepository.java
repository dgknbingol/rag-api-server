package com.aislam.rag.repository;

import com.aislam.rag.entity.ChatDailyUsageEntity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatDailyUsageRepository extends JpaRepository<ChatDailyUsageEntity, UUID> {

    Optional<ChatDailyUsageEntity> findByAppUserIdAndUsageDate(UUID appUserId, LocalDate usageDate);
}
