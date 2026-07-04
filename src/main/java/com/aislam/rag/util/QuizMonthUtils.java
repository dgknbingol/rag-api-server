package com.aislam.rag.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

public final class QuizMonthUtils {

    private QuizMonthUtils() {
    }

    public static YearMonth currentMonth(ZoneId zoneId) {
        return YearMonth.now(zoneId);
    }

    public static Instant startOfMonth(YearMonth month, ZoneId zoneId) {
        return month.atDay(1).atStartOfDay(zoneId).toInstant();
    }

    public static Instant startOfNextMonth(YearMonth month, ZoneId zoneId) {
        return month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
    }

    public static String monthKey(YearMonth month) {
        return month.toString();
    }

    public static String monthLabel(YearMonth month) {
        String monthName = month.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("tr"));
        return monthName.substring(0, 1).toUpperCase(new Locale("tr")) + monthName.substring(1)
                + " " + month.getYear();
    }

    public static LocalDate toLocalDate(Instant instant, ZoneId zoneId) {
        return instant.atZone(zoneId).toLocalDate();
    }
}
