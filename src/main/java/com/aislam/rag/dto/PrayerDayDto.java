package com.aislam.rag.dto;

public record PrayerDayDto(
        String date,
        String gregorianLabel,
        String hijriLabel,
        String imsak,
        String gunes,
        String ogle,
        String ikindi,
        String aksam,
        String yatsi,
        boolean today,
        String activeVakit,
        String activeSaat
) {
}
