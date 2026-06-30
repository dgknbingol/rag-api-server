package com.aislam.rag.service;

import com.aislam.rag.client.DiyanetPrayerTimesClient;
import com.aislam.rag.config.PrayerTimesProperties;
import com.aislam.rag.dto.MonthlyPrayerTimesResponse;
import com.aislam.rag.dto.PrayerDayDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class PrayerTimesService {

    private final DiyanetPrayerTimesClient diyanetClient;
    private final DistrictResolverService districtResolver;
    private final PrayerTimesMapper mapper;
    private final PrayerTimesCache cache;
    private final ZoneId zoneId;

    public PrayerTimesService(
            DiyanetPrayerTimesClient diyanetClient,
            DistrictResolverService districtResolver,
            PrayerTimesMapper mapper,
            PrayerTimesCache cache,
            PrayerTimesProperties properties
    ) {
        this.diyanetClient = diyanetClient;
        this.districtResolver = districtResolver;
        this.mapper = mapper;
        this.cache = cache;
        this.zoneId = properties.zoneId();
    }

    public MonthlyPrayerTimesResponse getMonthly(double latitude, double longitude, Integer year, Integer month) {
        YearMonth yearMonth = resolveYearMonth(year, month);
        String districtId = districtResolver.resolveDistrictId(latitude, longitude);

        List<PrayerDayDto> cachedDays = cache.get(districtId, yearMonth);
        if (cachedDays != null) {
            return new MonthlyPrayerTimesResponse(
                    yearMonth.getYear(),
                    yearMonth.getMonthValue(),
                    latitude,
                    longitude,
                    true,
                    cachedDays
            );
        }

        List<JsonNode> rawDays = diyanetClient.fetchMonthlyByDistrict(
                districtId,
                yearMonth.getYear(),
                yearMonth.getMonthValue()
        );
        List<PrayerDayDto> days = mapper.mapDiyanetDays(rawDays, zoneId);
        cache.put(districtId, yearMonth, days);

        return new MonthlyPrayerTimesResponse(
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                latitude,
                longitude,
                false,
                days
        );
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        YearMonth current = YearMonth.now(zoneId);
        int resolvedYear = year != null ? year : current.getYear();
        int resolvedMonth = month != null ? month : current.getMonthValue();
        return YearMonth.of(resolvedYear, resolvedMonth);
    }
}
