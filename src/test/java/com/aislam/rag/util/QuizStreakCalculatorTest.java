package com.aislam.rag.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuizStreakCalculatorTest {

    @Test
    void parseDailyEventDate_readsEventId() {
        assertEquals(LocalDate.of(2026, 7, 4), QuizStreakCalculator.parseDailyEventDate("daily-2026-07-04"));
        assertNull(QuizStreakCalculator.parseDailyEventDate("weekly-2026-07-04"));
    }

    @Test
    void calculateCurrentStreak_keepsYesterdayVisibleBeforeTodayIsPlayed() {
        LocalDate today = LocalDate.of(2026, 7, 4);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.add(LocalDate.of(2026, 7, 3));

        assertEquals(1, QuizStreakCalculator.calculateCurrentStreak(dates, today));
    }

    @Test
    void calculateCurrentStreak_countsTodayAndYesterday() {
        LocalDate today = LocalDate.of(2026, 7, 4);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.add(LocalDate.of(2026, 7, 4));
        dates.add(LocalDate.of(2026, 7, 3));

        assertEquals(2, QuizStreakCalculator.calculateCurrentStreak(dates, today));
    }

    @Test
    void calculateCurrentStreak_resetsAfterMissedDay() {
        LocalDate today = LocalDate.of(2026, 7, 5);
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.add(LocalDate.of(2026, 7, 3));

        assertEquals(0, QuizStreakCalculator.calculateCurrentStreak(dates, today));
    }
}
