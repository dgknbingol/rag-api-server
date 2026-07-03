package com.aislam.rag.service;

import com.aislam.rag.dto.QuizAnswerCheckResponse;
import com.aislam.rag.dto.QuizQuestionResponse;
import com.aislam.rag.entity.QuizOptionEntity;
import com.aislam.rag.entity.QuizQuestionEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class QuizQuestionService {

    private final QuizQuestionRepository quizQuestionRepository;

    public QuizQuestionService(QuizQuestionRepository quizQuestionRepository) {
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> listActiveQuestions() {
        return quizQuestionRepository.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(QuizQuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizQuestionResponse getActiveQuestion(UUID id) {
        QuizQuestionEntity question = quizQuestionRepository.findById(id)
                .filter(QuizQuestionEntity::isActive)
                .orElseThrow(() -> new RagException("Quiz question not found", "QUIZ_QUESTION_NOT_FOUND"));
        return QuizQuestionResponse.from(question);
    }

    @Transactional(readOnly = true)
    public long countActiveQuestions() {
        return quizQuestionRepository.findByActiveTrueOrderByCreatedAtAsc().size();
    }

    @Transactional(readOnly = true)
    public QuizAnswerCheckResponse checkAnswer(UUID questionId, UUID optionId) {
        QuizQuestionEntity question = quizQuestionRepository.findById(questionId)
                .filter(QuizQuestionEntity::isActive)
                .orElseThrow(() -> new RagException("Quiz question not found", "QUIZ_QUESTION_NOT_FOUND"));

        UUID correctOptionId = question.getOptions().stream()
                .filter(QuizOptionEntity::isCorrect)
                .map(QuizOptionEntity::getId)
                .findFirst()
                .orElseThrow(() -> new RagException("Quiz correct option not found", "QUIZ_OPTION_NOT_FOUND"));

        boolean correct = question.getOptions().stream()
                .anyMatch(option -> option.getId().equals(optionId) && option.isCorrect());

        if (question.getOptions().stream().noneMatch(option -> option.getId().equals(optionId))) {
            throw new RagException("Quiz option not found", "QUIZ_OPTION_NOT_FOUND");
        }

        return new QuizAnswerCheckResponse(correct, correctOptionId);
    }
}
