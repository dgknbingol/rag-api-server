package com.aislam.rag.dto;

import java.util.List;

public record MonthlyPrayerTimesResponse(
        int year,
        int month,
        double latitude,
        double longitude,
        boolean fromCache,
        List<PrayerDayDto> days
) {
}
