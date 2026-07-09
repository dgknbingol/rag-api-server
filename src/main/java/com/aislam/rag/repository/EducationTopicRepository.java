package com.aislam.rag.repository;

import com.aislam.rag.entity.EducationTopicEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EducationTopicRepository extends JpaRepository<EducationTopicEntity, UUID> {

  Optional<EducationTopicEntity> findBySlug(String slug);

  boolean existsBySlug(String slug);

  @EntityGraph(attributePaths = {"module", "module.category", "blocks"})
  Optional<EducationTopicEntity> findDetailedBySlug(String slug);
}
