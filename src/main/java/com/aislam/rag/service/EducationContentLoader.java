package com.aislam.rag.service;

import com.aislam.rag.dto.EducationCatalogSeedDto;
import com.aislam.rag.dto.EducationTopicContentDto;
import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;

@Component
public class EducationContentLoader {

    private static final Logger log = LoggerFactory.getLogger(EducationContentLoader.class);
    private static final String CATALOG_PATH = "education/catalog.json";
    private static final String TOPIC_CONTENT_PATH = "education/topics/%s.json";

    private final ObjectMapper objectMapper;
    private final EducationCatalogSeedDto catalog;

    public EducationContentLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.catalog = loadCatalog();
        log.info("Education catalog loaded version={} categories={}", catalog.version(), catalog.categories().size());
    }

    public EducationCatalogSeedDto catalog() {
        return catalog;
    }

    public Optional<EducationTopicContentDto> topicContent(String topicId) {
        String path = TOPIC_CONTENT_PATH.formatted(topicId);
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return Optional.empty();
        }

        try (InputStream input = resource.getInputStream()) {
            EducationTopicContentDto content = objectMapper.readValue(input, EducationTopicContentDto.class);
            if (content == null || content.blocks() == null || content.blocks().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(content);
        } catch (IOException ex) {
            throw new RagException("Failed to load education topic content: " + path, "EDUCATION_CONTENT_ERROR", ex);
        }
    }

    private EducationCatalogSeedDto loadCatalog() {
        ClassPathResource resource = new ClassPathResource(CATALOG_PATH);
        if (!resource.exists()) {
            log.warn("Education catalog file not found, using empty catalog");
            return new EducationCatalogSeedDto(1, Collections.emptyList());
        }

        try (InputStream input = resource.getInputStream()) {
            EducationCatalogSeedDto loaded = objectMapper.readValue(input, EducationCatalogSeedDto.class);
            if (loaded == null || loaded.categories() == null) {
                return new EducationCatalogSeedDto(1, Collections.emptyList());
            }
            return loaded;
        } catch (IOException ex) {
            throw new RagException("Failed to load education catalog", "EDUCATION_CONTENT_ERROR", ex);
        }
    }
}
