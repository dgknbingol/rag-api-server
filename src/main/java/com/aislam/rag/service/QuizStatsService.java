package com.aislam.rag.service;

import com.aislam.rag.config.DailyProperties;
import com.aislam.rag.dto.AchievementBadgeResponse;
import com.aislam.rag.dto.AchievementHighlightsResponse;
import com.aislam.rag.dto.LeaderboardEntryResponse;
import com.aislam.rag.dto.MonthlyLeaderboardResponse;
import com.aislam.rag.dto.MonthlyStatsResponse;
import com.aislam.rag.dto.QuizAchievementsResponse;
import com.aislam.rag.dto.QuizAttemptStatusResponse;
import com.aislam.rag.dto.QuizPlayerResponse;
import com.aislam.rag.dto.RegisterQuizPlayerRequest;
import com.aislam.rag.dto.SubmitQuizAttemptRequest;
import com.aislam.rag.entity.QuizAttemptEntity;
import com.aislam.rag.entity.QuizPlayerEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.QuizAttemptRepository;
import com.aislam.rag.repository.QuizPlayerRepository;
import com.aislam.rag.util.QuizMonthUtils;
import com.aislam.rag.util.QuizPlayerPresentation;
import com.aislam.rag.util.QuizStreakCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuizStatsService {

    private static final int DEFAULT_LEADERBOARD_LIMIT = 10;

    private final QuizPlayerRepository quizPlayerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ZoneId zoneId;

    public QuizStatsService(
            QuizPlayerRepository quizPlayerRepository,
            QuizAttemptRepository quizAttemptRepository,
            DailyProperties dailyProperties
    ) {
        this.quizPlayerRepository = quizPlayerRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.zoneId = dailyProperties.zoneId();
    }

    @Transactional
    public QuizPlayerResponse registerPlayer(RegisterQuizPlayerRequest request) {
        String displayName = sanitizeDisplayName(request.displayName());

        if (request.playerId() != null) {
            Optional<QuizPlayerEntity> existing = quizPlayerRepository.findById(request.playerId());
            if (existing.isPresent()) {
                QuizPlayerEntity player = existing.get();
                if (!displayName.equals(player.getDisplayName())) {
                    player.setDisplayName(displayName);
                }
                return toPlayerResponse(player);
            }
        }

        UUID playerId = request.playerId() != null ? request.playerId() : UUID.randomUUID();
        QuizPlayerEntity created = quizPlayerRepository.save(new QuizPlayerEntity(playerId, displayName));
        return toPlayerResponse(created);
    }

    @Transactional
    public QuizAttemptStatusResponse submitAttempt(SubmitQuizAttemptRequest request) {
        validateAttemptRequest(request);

        QuizPlayerEntity player = quizPlayerRepository.findById(request.playerId())
                .orElseThrow(() -> new RagException("Quiz player not found", "QUIZ_PLAYER_NOT_FOUND"));

        if (quizAttemptRepository.findByPlayerIdAndEventId(player.getId(), request.eventId()).isPresent()) {
            throw new RagException("Quiz attempt already submitted for this event", "QUIZ_ALREADY_ATTEMPTED");
        }

        quizAttemptRepository.save(new QuizAttemptEntity(
                player,
                request.eventId(),
                request.score(),
                request.correctCount(),
                request.questionCount()
        ));

        return new QuizAttemptStatusResponse(true, true, request.score(), request.correctCount(), request.questionCount());
    }

    @Transactional(readOnly = true)
    public QuizAttemptStatusResponse getAttemptStatus(UUID playerId, String eventId) {
        return quizAttemptRepository.findByPlayerIdAndEventId(playerId, eventId)
                .map(attempt -> new QuizAttemptStatusResponse(
                        true,
                        true,
                        attempt.getScore(),
                        attempt.getCorrectCount(),
                        attempt.getQuestionCount()
                ))
                .orElseGet(QuizAttemptStatusResponse::empty);
    }

    @Transactional(readOnly = true)
    public MonthlyLeaderboardResponse getMonthlyLeaderboard(UUID currentPlayerId, int limit) {
        YearMonth month = QuizMonthUtils.currentMonth(zoneId);
        List<QuizAttemptRepository.MonthlyAggregateRow> aggregates = loadMonthlyAggregates(month);
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<LeaderboardEntryResponse> entries = aggregates.stream()
                .limit(safeLimit)
                .map(row -> new LeaderboardEntryResponse(
                        row.getPlayerId(),
                        row.getDisplayName(),
                        row.getTotalScore(),
                        QuizPlayerPresentation.initials(row.getDisplayName()),
                        QuizPlayerPresentation.accent(row.getPlayerId())
                ))
                .toList();

        LeaderboardEntryResponse currentPlayer = null;
        int currentRank = 0;
        for (int index = 0; index < aggregates.size(); index += 1) {
            QuizAttemptRepository.MonthlyAggregateRow row = aggregates.get(index);
            if (row.getPlayerId().equals(currentPlayerId)) {
                currentRank = index + 1;
                currentPlayer = new LeaderboardEntryResponse(
                        row.getPlayerId(),
                        row.getDisplayName(),
                        row.getTotalScore(),
                        QuizPlayerPresentation.initials(row.getDisplayName()),
                        QuizPlayerPresentation.accent(row.getPlayerId())
                );
                break;
            }
        }

        return new MonthlyLeaderboardResponse(
                entries,
                currentPlayerId,
                currentPlayer,
                currentRank,
                aggregates.size()
        );
    }

    @Transactional(readOnly = true)
    public QuizAchievementsResponse getAchievements(UUID playerId) {
        quizPlayerRepository.findById(playerId)
                .orElseThrow(() -> new RagException("Quiz player not found", "QUIZ_PLAYER_NOT_FOUND"));

        YearMonth currentMonth = QuizMonthUtils.currentMonth(zoneId);
        List<MonthlyStatsResponse> monthlyHistory = new ArrayList<>();
        for (int offset = 0; offset < 3; offset += 1) {
            YearMonth month = currentMonth.minusMonths(offset);
            monthlyHistory.add(buildMonthlyStats(playerId, month, offset == 0));
        }

        List<QuizAttemptEntity> attempts = quizAttemptRepository.findByPlayerIdOrderByCompletedAtDesc(playerId);
        AchievementHighlightsResponse highlights = buildHighlights(playerId, attempts, monthlyHistory.get(0));
        List<AchievementBadgeResponse> badges = buildBadges(playerId, attempts, monthlyHistory);

        return new QuizAchievementsResponse(monthlyHistory, highlights, badges);
    }

    private List<QuizAttemptRepository.MonthlyAggregateRow> loadMonthlyAggregates(YearMonth month) {
        Instant start = QuizMonthUtils.startOfMonth(month, zoneId);
        Instant end = QuizMonthUtils.startOfNextMonth(month, zoneId);
        return quizAttemptRepository.findMonthlyAggregates(start, end);
    }

    private MonthlyStatsResponse buildMonthlyStats(UUID playerId, YearMonth month, boolean currentMonth) {
        List<QuizAttemptRepository.MonthlyAggregateRow> aggregates = loadMonthlyAggregates(month);
        int rank = 0;
        long totalScore = 0;
        int quizzesCompleted = 0;
        int bestDailyScore = 0;

        for (int index = 0; index < aggregates.size(); index += 1) {
            QuizAttemptRepository.MonthlyAggregateRow row = aggregates.get(index);
            if (row.getPlayerId().equals(playerId)) {
                rank = index + 1;
                totalScore = row.getTotalScore();
                quizzesCompleted = (int) row.getQuizzesCompleted();
                bestDailyScore = row.getBestDailyScore();
                break;
            }
        }

        return new MonthlyStatsResponse(
                QuizMonthUtils.monthKey(month),
                QuizMonthUtils.monthLabel(month),
                totalScore,
                rank,
                aggregates.size(),
                quizzesCompleted,
                bestDailyScore,
                currentMonth
        );
    }

    private AchievementHighlightsResponse buildHighlights(
            UUID playerId,
            List<QuizAttemptEntity> attempts,
            MonthlyStatsResponse currentMonth
    ) {
        int streak = calculateCurrentStreak(attempts);
        int bestRankThisYear = calculateBestRankThisYear(playerId);
        long lifetimeScore = attempts.stream().mapToLong(QuizAttemptEntity::getScore).sum();

        return new AchievementHighlightsResponse(
                streak,
                bestRankThisYear > 0 ? bestRankThisYear : currentMonth.rank(),
                lifetimeScore,
                currentMonth.quizzesCompleted()
        );
    }

    private int calculateBestRankThisYear(UUID playerId) {
        int year = QuizMonthUtils.currentMonth(zoneId).getYear();
        int bestRank = 0;

        for (int monthValue = 1; monthValue <= 12; monthValue += 1) {
            YearMonth month = YearMonth.of(year, monthValue);
            if (month.isAfter(QuizMonthUtils.currentMonth(zoneId))) {
                break;
            }
            MonthlyStatsResponse stats = buildMonthlyStats(playerId, month, false);
            if (stats.rank() > 0 && (bestRank == 0 || stats.rank() < bestRank)) {
                bestRank = stats.rank();
            }
        }

        return bestRank;
    }

    private int calculateCurrentStreak(List<QuizAttemptEntity> attempts) {
        Set<LocalDate> competitionDates = attempts.stream()
                .map(QuizStatsService::dailyCompetitionDateOf)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return QuizStreakCalculator.calculateCurrentStreak(
                competitionDates,
                LocalDate.now(zoneId)
        );
    }

    private static Optional<LocalDate> dailyCompetitionDateOf(QuizAttemptEntity attempt) {
        LocalDate fromEventId = QuizStreakCalculator.parseDailyEventDate(attempt.getEventId());
        if (fromEventId != null) {
            return Optional.of(fromEventId);
        }
        return Optional.empty();
    }

    private List<AchievementBadgeResponse> buildBadges(
            UUID playerId,
            List<QuizAttemptEntity> attempts,
            List<MonthlyStatsResponse> monthlyHistory
    ) {
        int streak = calculateCurrentStreak(attempts);
        boolean perfectDay = attempts.stream()
                .anyMatch(attempt -> attempt.getCorrectCount() == attempt.getQuestionCount()
                        && attempt.getQuestionCount() > 0);
        boolean topTwenty = monthlyHistory.stream().anyMatch(stats -> stats.rank() > 0 && stats.rank() <= 20);
        boolean monthlyChampion = monthlyHistory.stream().anyMatch(stats -> stats.rank() == 1);

        return List.of(
                new AchievementBadgeResponse(
                        "streak-5",
                        "5 Gün Serisi",
                        "5 gün üst üste yarışmaya katıl",
                        "flame",
                        streak >= 5
                ),
                new AchievementBadgeResponse(
                        "top-20",
                        "İlk 20",
                        "Ay içinde ilk 20'ye gir",
                        "medal",
                        topTwenty
                ),
                new AchievementBadgeResponse(
                        "perfect-day",
                        "Mükemmel Gün",
                        "Bir yarışmada tüm soruları doğru yanıtla",
                        "star",
                        perfectDay
                ),
                new AchievementBadgeResponse(
                        "monthly-champion",
                        "Ayın Birincisi",
                        "Aylık sıralamada 1. ol",
                        "trophy",
                        monthlyChampion
                )
        );
    }

    private void validateAttemptRequest(SubmitQuizAttemptRequest request) {
        if (request.playerId() == null) {
            throw new RagException("playerId is required", "QUIZ_VALIDATION_ERROR");
        }
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new RagException("eventId is required", "QUIZ_VALIDATION_ERROR");
        }
        if (request.score() < 0 || request.correctCount() < 0 || request.questionCount() <= 0) {
            throw new RagException("Invalid attempt payload", "QUIZ_VALIDATION_ERROR");
        }
        if (request.correctCount() > request.questionCount()) {
            throw new RagException("correctCount cannot exceed questionCount", "QUIZ_VALIDATION_ERROR");
        }
    }

    private String sanitizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "Oyuncu";
        }
        return displayName.trim().substring(0, Math.min(displayName.trim().length(), 80));
    }

    private QuizPlayerResponse toPlayerResponse(QuizPlayerEntity player) {
        return new QuizPlayerResponse(player.getId(), player.getDisplayName());
    }
}
