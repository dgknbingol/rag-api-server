package com.aislam.rag.service;

import com.aislam.rag.dto.EducationBlockDto;
import com.aislam.rag.dto.EducationCatalogSeedDto;
import com.aislam.rag.dto.EducationCategorySeedDto;
import com.aislam.rag.dto.EducationModuleSeedDto;
import com.aislam.rag.dto.EducationQuizSeedDto;
import com.aislam.rag.dto.EducationTopicContentDto;
import com.aislam.rag.dto.EducationTopicSeedDto;
import com.aislam.rag.entity.EducationBlockEntity;
import com.aislam.rag.entity.EducationCatalogMetaEntity;
import com.aislam.rag.entity.EducationCategoryEntity;
import com.aislam.rag.entity.EducationModuleEntity;
import com.aislam.rag.entity.EducationQuizEntity;
import com.aislam.rag.entity.EducationTopicEntity;
import com.aislam.rag.repository.EducationCatalogMetaRepository;
import com.aislam.rag.repository.EducationCategoryRepository;
import com.aislam.rag.repository.EducationModuleRepository;
import com.aislam.rag.repository.EducationQuizRepository;
import com.aislam.rag.repository.EducationTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EducationSeedService implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(EducationSeedService.class);

  private final EducationContentLoader educationContentLoader;
  private final EducationCatalogMetaRepository catalogMetaRepository;
  private final EducationCategoryRepository categoryRepository;
  private final EducationModuleRepository moduleRepository;
  private final EducationTopicRepository topicRepository;
  private final EducationQuizRepository quizRepository;

  public EducationSeedService(
      EducationContentLoader educationContentLoader,
      EducationCatalogMetaRepository catalogMetaRepository,
      EducationCategoryRepository categoryRepository,
      EducationModuleRepository moduleRepository,
      EducationTopicRepository topicRepository,
      EducationQuizRepository quizRepository) {
    this.educationContentLoader = educationContentLoader;
    this.catalogMetaRepository = catalogMetaRepository;
    this.categoryRepository = categoryRepository;
    this.moduleRepository = moduleRepository;
    this.topicRepository = topicRepository;
    this.quizRepository = quizRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    EducationCatalogSeedDto catalog = educationContentLoader.catalog();
    if (catalog.categories().isEmpty()) {
      log.info("Education seed skipped: catalog has no categories");
      return;
    }

    updateCatalogVersion(catalog.version());

    int categoriesCreated = 0;
    int modulesCreated = 0;
    int topicsCreated = 0;
    int blocksImported = 0;
    int topicsSkipped = 0;

    for (int categoryIndex = 0; categoryIndex < catalog.categories().size(); categoryIndex++) {
      EducationCategorySeedDto categorySeed = catalog.categories().get(categoryIndex);
      final int categorySortOrder = categoryIndex;

      EducationCategoryEntity category =
          categoryRepository.findBySlug(categorySeed.id()).orElse(null);
      if (category == null) {
        categoriesCreated++;
        category =
            categoryRepository.save(
                new EducationCategoryEntity(
                    categorySeed.id(),
                    categorySeed.title(),
                    categorySeed.subtitle(),
                    categorySeed.icon(),
                    categorySortOrder));
      }

      seedQuiz(
          categorySeed.quiz(),
          EducationQuizEntity.TYPE_CATEGORY,
          categorySeed.id(),
          categorySeed.title() + " Genel Quiz");

      for (int moduleIndex = 0; moduleIndex < categorySeed.modules().size(); moduleIndex++) {
        EducationModuleSeedDto moduleSeed = categorySeed.modules().get(moduleIndex);
        final int moduleSortOrder = moduleIndex;

        EducationModuleEntity module =
            moduleRepository.findBySlug(moduleSeed.id()).orElse(null);
        if (module == null) {
          modulesCreated++;
          module =
              new EducationModuleEntity(
                  moduleSeed.id(), moduleSeed.title(), moduleSeed.summary(), moduleSortOrder);
          category.addModule(module);
          module = moduleRepository.save(module);
        }

        seedQuiz(
            moduleSeed.quiz(),
            EducationQuizEntity.TYPE_MODULE,
            moduleSeed.id(),
            moduleSeed.title() + " Mini Quiz");

        for (int topicIndex = 0; topicIndex < moduleSeed.lessons().size(); topicIndex++) {
          EducationTopicSeedDto topicSeed = moduleSeed.lessons().get(topicIndex);
          final int topicSortOrder = topicIndex;

          var existingTopic = topicRepository.findBySlug(topicSeed.id());
          if (existingTopic.isPresent() && existingTopic.get().hasContent()) {
            topicsSkipped++;
            continue;
          }

          EducationTopicEntity topic = existingTopic.orElse(null);
          if (topic == null) {
            topicsCreated++;
            topic =
                new EducationTopicEntity(
                    topicSeed.id(), topicSeed.title(), topicSeed.summary(), null, topicSortOrder);
            module.addTopic(topic);
            topic = topicRepository.save(topic);
          }

          var content = educationContentLoader.topicContent(topicSeed.id());
          if (content.isEmpty()) {
            continue;
          }

          applyContent(topic, content.get());
          topicRepository.save(topic);
          blocksImported += topic.getBlocks().size();
        }
      }
    }

    log.info(
        "Education seed complete categoriesCreated={} modulesCreated={} topicsCreated={} blocksImported={} topicsSkipped={}",
        categoriesCreated,
        modulesCreated,
        topicsCreated,
        blocksImported,
        topicsSkipped);
  }

  private void seedQuiz(
      EducationQuizSeedDto quizSeed, String quizType, String scopeSlug, String fallbackTitle) {
    if (quizSeed == null) {
      return;
    }

    if (quizRepository.findBySlug(quizSeed.id()).isPresent()) {
      return;
    }

    quizRepository.save(
        new EducationQuizEntity(
            quizSeed.id(),
            quizSeed.title() != null && !quizSeed.title().isBlank() ? quizSeed.title() : fallbackTitle,
            quizType,
            scopeSlug,
            quizSeed.questionCount(),
            quizSeed.passPercent(),
            quizSeed.achievementId()));
  }

  private void updateCatalogVersion(int version) {
    EducationCatalogMetaEntity meta =
        catalogMetaRepository
            .findById(EducationCatalogMetaEntity.SINGLETON_ID)
            .orElseGet(() -> new EducationCatalogMetaEntity(version));
    meta.setVersion(version);
    catalogMetaRepository.save(meta);
  }

  private void applyContent(EducationTopicEntity topic, EducationTopicContentDto content) {
    topic.getBlocks().clear();
    if (content.readingMinutes() != null) {
      topic.setReadingMinutes(content.readingMinutes());
    }

    int blockOrder = 0;
    for (EducationBlockDto block : content.blocks()) {
      topic.addBlock(
          new EducationBlockEntity(
              block.type(),
              block.text(),
              block.title(),
              block.url(),
              block.items(),
              blockOrder++));
    }
    topic.markContentReady();
  }
}
