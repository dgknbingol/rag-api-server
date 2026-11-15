package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.MonthlyLeaderboardResponse;
import com.aislam.rag.dto.QuizAchievementsResponse;
import com.aislam.rag.dto.QuizAttemptStatusResponse;
import com.aislam.rag.dto.QuizParticipationResponse;
import com.aislam.rag.dto.QuizPlayerResponse;
import com.aislam.rag.dto.RegisterQuizParticipationRequest;
import com.aislam.rag.dto.RegisterQuizPlayerRequest;
import com.aislam.rag.dto.SubmitQuizAttemptRequest;
import com.aislam.rag.security.RequestAppUserId;
import com.aislam.rag.service.QuizStatsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/quiz", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class QuizStatsController {

    private final QuizStatsService quizStatsService;

    public QuizStatsController(QuizStatsService quizStatsService) {
        this.quizStatsService = quizStatsService;
    }

    @PostMapping("/players/register")
    public ResponseEntity<QuizPlayerResponse> registerPlayer(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody RegisterQuizPlayerRequest request
    ) {
        RequestAppUserId.require(appUserIdHeader);
        return ResponseEntity.ok(quizStatsService.registerPlayer(request));
    }

    @PostMapping("/participations")
    public ResponseEntity<QuizParticipationResponse> registerParticipation(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody RegisterQuizParticipationRequest request
    ) {
        return ResponseEntity.ok(
                quizStatsService.registerParticipation(RequestAppUserId.require(appUserIdHeader), request)
        );
    }

    @PostMapping("/attempts")
    public ResponseEntity<QuizAttemptStatusResponse> submitAttempt(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody SubmitQuizAttemptRequest request
    ) {
        RequestAppUserId.require(appUserIdHeader);
        return ResponseEntity.ok(quizStatsService.submitAttempt(request));
    }

    @GetMapping("/attempts/status")
    public ResponseEntity<QuizAttemptStatusResponse> getAttemptStatus(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @RequestParam java.util.UUID playerId,
            @RequestParam String eventId
    ) {
        RequestAppUserId.require(appUserIdHeader);
        return ResponseEntity.ok(quizStatsService.getAttemptStatus(playerId, eventId));
    }

    @GetMapping("/leaderboard/monthly")
    public ResponseEntity<MonthlyLeaderboardResponse> getMonthlyLeaderboard(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @RequestParam java.util.UUID playerId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        RequestAppUserId.require(appUserIdHeader);
        return ResponseEntity.ok(quizStatsService.getMonthlyLeaderboard(playerId, limit));
    }

    @GetMapping("/achievements")
    public ResponseEntity<QuizAchievementsResponse> getAchievements(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @RequestParam java.util.UUID playerId
    ) {
        RequestAppUserId.require(appUserIdHeader);
        return ResponseEntity.ok(quizStatsService.getAchievements(playerId));
    }
}
