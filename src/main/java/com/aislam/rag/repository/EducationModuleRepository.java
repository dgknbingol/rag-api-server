package com.aislam.rag.repository;

import com.aislam.rag.entity.EducationModuleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EducationModuleRepository extends JpaRepository<EducationModuleEntity, UUID> {

  Optional<EducationModuleEntity> findBySlug(String slug);

  @EntityGraph(attributePaths = {"category", "topics"})
  Optional<EducationModuleEntity> findDetailedBySlug(String slug);
}
