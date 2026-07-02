package com.aislam.rag.repository;

import com.aislam.rag.entity.QuizQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestionEntity, UUID> {

    boolean existsByQuestionTextIgnoreCase(String questionText);

    Optional<QuizQuestionEntity> findByQuestionTextIgnoreCase(String questionText);

    List<QuizQuestionEntity> findByActiveTrueOrderByCreatedAtAsc();
}
