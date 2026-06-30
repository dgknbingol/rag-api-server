package com.aislam.rag.service;

import com.aislam.rag.dto.PrayerDayDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class PrayerTimesMapper {

    private static final Locale TR = Locale.forLanguageTag("tr-TR");
    private static final DateTimeFormatter GREGORIAN_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy EEEE", TR);

    private record PrayerSlot(String label, String jsonKey) {
    }

    private static final List<PrayerSlot> PRAYER_SLOTS = List.of(
            new PrayerSlot("İmsak", "imsak"),
            new PrayerSlot("Güneş", "gunes"),
            new PrayerSlot("Öğle", "ogle"),
            new PrayerSlot("İkindi", "ikindi"),
            new PrayerSlot("Akşam", "aksam"),
            new PrayerSlot("Yatsı", "yatsi")
    );

    public List<PrayerDayDto> mapDiyanetDays(List<JsonNode> rawDays, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId);

        return rawDays.stream()
                .map(day -> mapDiyanetDay(day, today, now, zoneId))
                .toList();
    }

    private PrayerDayDto mapDiyanetDay(JsonNode day, LocalDate today, LocalTime now, ZoneId zoneId) {
        LocalDate date = parseDate(day.path("date").asText(""), zoneId);
        JsonNode times = day.path("times");

        String imsak = times.path("imsak").asText("");
        String gunes = times.path("gunes").asText("");
        String ogle = times.path("ogle").asText("");
        String ikindi = times.path("ikindi").asText("");
        String aksam = times.path("aksam").asText("");
        String yatsi = times.path("yatsi").asText("");

        String hijriLabel = day.path("hijri_date").path("full_date").asText("");
        String gregorianLabel = capitalize(GREGORIAN_FORMAT.format(date));

        boolean isToday = date.equals(today);
        String activeVakit = null;
        String activeSaat = null;
        if (isToday) {
            var active = resolveActivePrayer(times, now);
            activeVakit = active.label();
            activeSaat = active.time();
        }

        return new PrayerDayDto(
                date.toString(),
                gregorianLabel,
                hijriLabel,
                imsak,
                gunes,
                ogle,
                ikindi,
                aksam,
                yatsi,
                isToday,
                activeVakit,
                activeSaat
        );
    }

    private record ActivePrayer(String label, String time) {
    }

    private ActivePrayer resolveActivePrayer(JsonNode times, LocalTime now) {
        PrayerSlot next = PRAYER_SLOTS.getFirst();
        String nextTime = times.path(next.jsonKey()).asText("");

        for (PrayerSlot slot : PRAYER_SLOTS) {
            String timeText = times.path(slot.jsonKey()).asText("");
            if (timeText.isBlank()) {
                continue;
            }
            LocalTime slotTime = LocalTime.parse(timeText);
            if (now.isBefore(slotTime)) {
                return new ActivePrayer(slot.label(), timeText);
            }
            next = slot;
            nextTime = timeText;
        }

        return new ActivePrayer(next.label(), nextTime);
    }

    private static LocalDate parseDate(String rawDate, ZoneId zoneId) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.EPOCH;
        }
        return Instant.parse(rawDate).atZone(zoneId).toLocalDate();
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(TR) + text.substring(1);
    }
}
