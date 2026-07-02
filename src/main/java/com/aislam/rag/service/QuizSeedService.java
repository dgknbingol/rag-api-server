package com.aislam.rag.service;

import com.aislam.rag.dto.QuizSeedQuestionDto;
import com.aislam.rag.entity.QuizOptionEntity;
import com.aislam.rag.entity.QuizQuestionEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.QuizQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class QuizSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizSeedService.class);
    private static final String QUESTIONS_PATH = "quiz/questions.json";

    private final ObjectMapper objectMapper;
    private final QuizQuestionRepository quizQuestionRepository;

    public QuizSeedService(ObjectMapper objectMapper, QuizQuestionRepository quizQuestionRepository) {
        this.objectMapper = objectMapper;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<QuizSeedQuestionDto> seeds = loadSeeds();
        int imported = 0;
        int skipped = 0;

        for (QuizSeedQuestionDto seed : seeds) {
            validateSeed(seed);
            if (quizQuestionRepository.existsByQuestionTextIgnoreCase(seed.question().trim())) {
                skipped++;
                continue;
            }
            quizQuestionRepository.save(toEntity(seed));
            imported++;
        }

        log.info("Quiz seed complete imported={} skipped={} totalInDb={}", imported, skipped, quizQuestionRepository.count());
    }

    private List<QuizSeedQuestionDto> loadSeeds() {
        try (InputStream input = new ClassPathResource(QUESTIONS_PATH).getInputStream()) {
            List<QuizSeedQuestionDto> items = objectMapper.readValue(input, new TypeReference<>() {});
            if (items == null || items.isEmpty()) {
                throw new RagException("Quiz seed file is empty: " + QUESTIONS_PATH, "QUIZ_SEED_ERROR");
            }
            return items;
        } catch (IOException ex) {
            throw new RagException("Failed to load quiz seed file: " + QUESTIONS_PATH, "QUIZ_SEED_ERROR", ex);
        }
    }

    private void validateSeed(QuizSeedQuestionDto seed) {
        if (seed.question() == null || seed.question().isBlank()) {
            throw new RagException("Quiz seed question text is required", "QUIZ_SEED_ERROR");
        }
        if (seed.category() == null || seed.category().isBlank()) {
            throw new RagException("Quiz seed category is required", "QUIZ_SEED_ERROR");
        }
        if (seed.options() == null || seed.options().size() != 4) {
            throw new RagException("Quiz seed must contain exactly 4 options", "QUIZ_SEED_ERROR");
        }

        long correctCount = seed.options().stream().filter(option -> option.correct()).count();
        if (correctCount != 1) {
            throw new RagException("Quiz seed must contain exactly 1 correct option", "QUIZ_SEED_ERROR");
        }
    }

    private QuizQuestionEntity toEntity(QuizSeedQuestionDto seed) {
        QuizQuestionEntity question = new QuizQuestionEntity(
                seed.question().trim(),
                seed.category().trim().toLowerCase()
        );

        for (int i = 0; i < seed.options().size(); i++) {
            var option = seed.options().get(i);
            question.addOption(new QuizOptionEntity(
                    option.label().trim().toUpperCase(),
                    option.text().trim(),
                    option.correct(),
                    i + 1
            ));
        }

        return question;
    }
}
