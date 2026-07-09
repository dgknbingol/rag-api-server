package com.aislam.rag.repository;

import com.aislam.rag.entity.EducationQuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EducationQuizRepository extends JpaRepository<EducationQuizEntity, UUID> {

  Optional<EducationQuizEntity> findBySlug(String slug);

  Optional<EducationQuizEntity> findByQuizTypeAndScopeSlug(String quizType, String scopeSlug);
}
