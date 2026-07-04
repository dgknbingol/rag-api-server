package com.aislam.rag.repository;

import com.aislam.rag.entity.QuizPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizPlayerRepository extends JpaRepository<QuizPlayerEntity, UUID> {
}
