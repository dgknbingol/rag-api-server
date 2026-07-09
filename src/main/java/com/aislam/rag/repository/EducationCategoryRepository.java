package com.aislam.rag.repository;

import com.aislam.rag.entity.EducationCategoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationCategoryRepository extends JpaRepository<EducationCategoryEntity, UUID> {

  Optional<EducationCategoryEntity> findBySlug(String slug);

  @EntityGraph(attributePaths = "modules")
  List<EducationCategoryEntity> findAllByOrderBySortOrderAsc();
}
