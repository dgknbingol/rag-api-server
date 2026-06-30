package com.aislam.rag.service;

import com.aislam.rag.config.DailyProperties;
import com.aislam.rag.dto.DailyContentResponse;
import com.aislam.rag.dto.DailyItemDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class DailyContentService {

    private final DailyContentLoader loader;
    private final DailyProperties dailyProperties;
    private volatile CachedDaily cached;

    public DailyContentService(DailyContentLoader loader, DailyProperties dailyProperties) {
        this.loader = loader;
        this.dailyProperties = dailyProperties;
    }

    public DailyContentResponse getToday() {
        LocalDate today = LocalDate.now(dailyProperties.zoneId());
        CachedDaily current = cached;
        if (current != null && current.date.equals(today)) {
            return current.response;
        }

        synchronized (this) {
            current = cached;
            if (current != null && current.date.equals(today)) {
                return current.response;
            }

            DailyContentResponse response = pickForDate(today);
            cached = new CachedDaily(today, response);
            return response;
        }
    }

    DailyContentResponse pickForDate(LocalDate date) {
        Random random = new Random(date.toEpochDay());

        DailyItemDto ayet = pickRandom(loader.ayetler(), random);
        DailyItemDto dua = pickRandom(loader.dualar(), random);
        DailyItemDto hadis = pickRandom(loader.hadisler(), random);

        return new DailyContentResponse(date.toString(), ayet, dua, hadis);
    }

    private static DailyItemDto pickRandom(List<DailyItemDto> items, Random random) {
        return items.get(random.nextInt(items.size()));
    }

    private record CachedDaily(LocalDate date, DailyContentResponse response) {
    }
}
