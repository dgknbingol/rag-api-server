package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.MonthlyLeaderboardResponse;
import com.aislam.rag.dto.QuizAchievementsResponse;
import com.aislam.rag.dto.QuizAttemptStatusResponse;
import com.aislam.rag.dto.QuizPlayerResponse;
import com.aislam.rag.dto.RegisterQuizPlayerRequest;
import com.aislam.rag.dto.SubmitQuizAttemptRequest;
import com.aislam.rag.service.QuizStatsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/quiz", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class QuizStatsController {

    private final QuizStatsService quizStatsService;

    public QuizStatsController(QuizStatsService quizStatsService) {
        this.quizStatsService = quizStatsService;
    }

    @PostMapping("/players/register")
    public ResponseEntity<QuizPlayerResponse> registerPlayer(@Valid @RequestBody RegisterQuizPlayerRequest request) {
        return ResponseEntity.ok(quizStatsService.registerPlayer(request));
    }

    @PostMapping("/attempts")
    public ResponseEntity<QuizAttemptStatusResponse> submitAttempt(@Valid @RequestBody SubmitQuizAttemptRequest request) {
        return ResponseEntity.ok(quizStatsService.submitAttempt(request));
    }

    @GetMapping("/attempts/status")
    public ResponseEntity<QuizAttemptStatusResponse> getAttemptStatus(
            @RequestParam UUID playerId,
            @RequestParam String eventId
    ) {
        return ResponseEntity.ok(quizStatsService.getAttemptStatus(playerId, eventId));
    }

    @GetMapping("/leaderboard/monthly")
    public ResponseEntity<MonthlyLeaderboardResponse> getMonthlyLeaderboard(
            @RequestParam UUID playerId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(quizStatsService.getMonthlyLeaderboard(playerId, limit));
    }

    @GetMapping("/achievements")
    public ResponseEntity<QuizAchievementsResponse> getAchievements(@RequestParam UUID playerId) {
        return ResponseEntity.ok(quizStatsService.getAchievements(playerId));
    }
}
