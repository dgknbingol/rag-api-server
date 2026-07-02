package com.aislam.rag.service;

import com.aislam.rag.dto.QuizQuestionResponse;
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
}
