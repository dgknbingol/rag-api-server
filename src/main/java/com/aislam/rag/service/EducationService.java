package com.aislam.rag.service;

import com.aislam.rag.dto.EducationBlockDto;
import com.aislam.rag.dto.EducationCatalogResponse;
import com.aislam.rag.dto.EducationCategoryDto;
import com.aislam.rag.dto.EducationModuleDto;
import com.aislam.rag.dto.EducationQuizDto;
import com.aislam.rag.dto.EducationTopicContentDto;
import com.aislam.rag.dto.EducationTopicDetailResponse;
import com.aislam.rag.dto.EducationTopicSummaryDto;
import com.aislam.rag.entity.EducationBlockEntity;
import com.aislam.rag.entity.EducationCatalogMetaEntity;
import com.aislam.rag.entity.EducationCategoryEntity;
import com.aislam.rag.entity.EducationModuleEntity;
import com.aislam.rag.entity.EducationQuizEntity;
import com.aislam.rag.entity.EducationTopicEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.EducationCatalogMetaRepository;
import com.aislam.rag.repository.EducationCategoryRepository;
import com.aislam.rag.repository.EducationQuizRepository;
import com.aislam.rag.repository.EducationTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EducationService {

  private final EducationCatalogMetaRepository catalogMetaRepository;
  private final EducationCategoryRepository categoryRepository;
  private final EducationTopicRepository topicRepository;
  private final EducationQuizRepository quizRepository;

  public EducationService(
      EducationCatalogMetaRepository catalogMetaRepository,
      EducationCategoryRepository categoryRepository,
      EducationTopicRepository topicRepository,
      EducationQuizRepository quizRepository) {
    this.catalogMetaRepository = catalogMetaRepository;
    this.categoryRepository = categoryRepository;
    this.topicRepository = topicRepository;
    this.quizRepository = quizRepository;
  }

  @Transactional(readOnly = true)
  public EducationCatalogResponse getCatalog() {
    List<EducationCategoryDto> categories =
        categoryRepository.findAllByOrderBySortOrderAsc().stream()
            .map(this::toCategoryDto)
            .toList();
    return new EducationCatalogResponse(resolveCatalogVersion(), categories);
  }

  @Transactional(readOnly = true)
  public EducationTopicDetailResponse getTopic(String topicId) {
    EducationTopicEntity topic =
        topicRepository
            .findDetailedBySlug(topicId)
            .orElseThrow(
                () ->
                    new RagException(
                        "Education topic not found: " + topicId, "EDUCATION_TOPIC_NOT_FOUND"));

    EducationModuleEntity module = topic.getModule();
    EducationCategoryEntity category = module.getCategory();
    return new EducationTopicDetailResponse(
        topic.getSlug(),
        category.getSlug(),
        category.getTitle(),
        module.getSlug(),
        module.getTitle(),
        topic.getTitle(),
        topic.getSummary(),
        topic.hasContent() ? toTopicContent(topic) : null);
  }

  private int resolveCatalogVersion() {
    return catalogMetaRepository
        .findById(EducationCatalogMetaEntity.SINGLETON_ID)
        .map(EducationCatalogMetaEntity::getVersion)
        .orElse(1);
  }

  private EducationCategoryDto toCategoryDto(EducationCategoryEntity category) {
    List<EducationModuleDto> modules =
        category.getModules().stream().map(this::toModuleDto).toList();
    return new EducationCategoryDto(
        category.getSlug(),
        category.getTitle(),
        category.getSubtitle(),
        category.getIcon(),
        findQuiz(EducationQuizEntity.TYPE_CATEGORY, category.getSlug()),
        modules);
  }

  private EducationModuleDto toModuleDto(EducationModuleEntity module) {
    List<EducationTopicSummaryDto> lessons =
        module.getTopics().stream()
            .map(
                topic ->
                    new EducationTopicSummaryDto(
                        topic.getSlug(),
                        topic.getTitle(),
                        topic.getSummary(),
                        topic.hasContent()))
            .toList();
    return new EducationModuleDto(
        module.getSlug(),
        module.getTitle(),
        module.getSummary(),
        findQuiz(EducationQuizEntity.TYPE_MODULE, module.getSlug()),
        lessons);
  }

  private EducationQuizDto findQuiz(String quizType, String scopeSlug) {
    return quizRepository
        .findByQuizTypeAndScopeSlug(quizType, scopeSlug)
        .map(this::toQuizDto)
        .orElse(null);
  }

  private EducationQuizDto toQuizDto(EducationQuizEntity quiz) {
    return new EducationQuizDto(
        quiz.getSlug(),
        quiz.getTitle(),
        quiz.getQuizType(),
        quiz.getQuestionCount(),
        quiz.getPassPercent(),
        quiz.getAchievementId());
  }

  private EducationTopicContentDto toTopicContent(EducationTopicEntity topic) {
    List<EducationBlockDto> blocks =
        topic.getBlocks().stream().map(this::toBlockDto).toList();
    return new EducationTopicContentDto(topic.getReadingMinutes(), blocks);
  }

  private EducationBlockDto toBlockDto(EducationBlockEntity block) {
    List<String> items = block.getItems().isEmpty() ? null : block.getItems();
    return new EducationBlockDto(
        block.getType(),
        block.getText(),
        block.getTitle(),
        block.getUrl(),
        items);
  }
}
