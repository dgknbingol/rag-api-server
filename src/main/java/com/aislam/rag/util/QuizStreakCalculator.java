package com.aislam.rag.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

public final class QuizStreakCalculator {

    public static final String DAILY_EVENT_PREFIX = "daily-";

    private QuizStreakCalculator() {
    }

    public static LocalDate parseDailyEventDate(String eventId) {
        if (eventId == null || !eventId.startsWith(DAILY_EVENT_PREFIX)) {
            return null;
        }
        try {
            return LocalDate.parse(eventId.substring(DAILY_EVENT_PREFIX.length()));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Counts consecutive daily competition days ending at {@code today} if played today,
     * otherwise ending at yesterday so the streak stays visible until today's event is missed.
     */
    public static int calculateCurrentStreak(Set<LocalDate> dailyCompetitionDates, LocalDate today) {
        if (dailyCompetitionDates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = dailyCompetitionDates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (dailyCompetitionDates.contains(cursor)) {
            streak += 1;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
