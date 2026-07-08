package com.aislam.rag.repository;

import com.aislam.rag.entity.QuizParticipationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizParticipationRepository extends JpaRepository<QuizParticipationEntity, UUID> {

    Optional<QuizParticipationEntity> findByPlayerIdAndEventId(UUID playerId, String eventId);
}
