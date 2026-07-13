package com.aislam.rag.repository;

import com.aislam.rag.entity.ChatConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, UUID> {

    List<ChatConversationEntity> findByAppUserIdOrderByUpdatedAtDesc(UUID appUserId);

    Optional<ChatConversationEntity> findByIdAndAppUserId(UUID id, UUID appUserId);
}
