package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.QuizAnswerCheckRequest;
import com.aislam.rag.dto.QuizAnswerCheckResponse;
import com.aislam.rag.dto.QuizQuestionResponse;
import com.aislam.rag.service.QuizQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/quiz", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class QuizController {

    private final QuizQuestionService quizQuestionService;

    public QuizController(QuizQuestionService quizQuestionService) {
        this.quizQuestionService = quizQuestionService;
    }

    @GetMapping("/questions/count")
    public ResponseEntity<Map<String, Long>> countQuestions() {
        return ResponseEntity.ok(Map.of("count", quizQuestionService.countActiveQuestions()));
    }

    @GetMapping("/questions")
    public ResponseEntity<List<QuizQuestionResponse>> listQuestions() {
        return ResponseEntity.ok(quizQuestionService.listActiveQuestions());
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<QuizQuestionResponse> getQuestion(@PathVariable UUID id) {
        return ResponseEntity.ok(quizQuestionService.getActiveQuestion(id));
    }

    @PostMapping("/questions/{id}/check")
    public ResponseEntity<QuizAnswerCheckResponse> checkAnswer(
            @PathVariable UUID id,
            @RequestBody QuizAnswerCheckRequest request
    ) {
        return ResponseEntity.ok(quizQuestionService.checkAnswer(id, request.optionId()));
    }
}
