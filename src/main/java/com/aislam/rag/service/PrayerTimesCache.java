package com.aislam.rag.service;

import com.aislam.rag.dto.PrayerDayDto;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrayerTimesCache {

    private record CacheKey(String districtId, int year, int month) {
        static CacheKey of(String districtId, YearMonth yearMonth) {
            return new CacheKey(districtId, yearMonth.getYear(), yearMonth.getMonthValue());
        }
    }

    private record CacheEntry(List<PrayerDayDto> days) {
    }

    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public List<PrayerDayDto> get(String districtId, YearMonth yearMonth) {
        CacheEntry entry = cache.get(CacheKey.of(districtId, yearMonth));
        return entry == null ? null : entry.days();
    }

    public void put(String districtId, YearMonth yearMonth, List<PrayerDayDto> days) {
        cache.put(CacheKey.of(districtId, yearMonth), new CacheEntry(List.copyOf(days)));
    }
}
