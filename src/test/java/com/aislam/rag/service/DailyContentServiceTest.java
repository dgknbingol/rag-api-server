package com.aislam.rag.service;

import com.aislam.rag.config.DailyProperties;
import com.aislam.rag.dto.DailyContentResponse;
import com.aislam.rag.dto.DailyItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyContentServiceTest {

    private DailyContentLoader loader;
    private DailyContentService service;

    @BeforeEach
    void setUp() {
        loader = mock(DailyContentLoader.class);
        when(loader.ayetler()).thenReturn(java.util.List.of(
                new DailyItemDto("Ayet 1", "Ref 1"),
                new DailyItemDto("Ayet 2", "Ref 2"),
                new DailyItemDto("Ayet 3", "Ref 3")
        ));
        when(loader.dualar()).thenReturn(java.util.List.of(
                new DailyItemDto("Dua 1", "Ref D1"),
                new DailyItemDto("Dua 2", "Ref D2")
        ));
        when(loader.hadisler()).thenReturn(java.util.List.of(
                new DailyItemDto("Hadis 1", "Ref H1"),
                new DailyItemDto("Hadis 2", "Ref H2")
        ));

        service = new DailyContentService(loader, new DailyProperties("Europe/Istanbul"));
    }

    @Test
    void pickForDate_isDeterministicForSameDay() {
        LocalDate date = LocalDate.of(2026, 6, 4);

        DailyContentResponse first = service.pickForDate(date);
        DailyContentResponse second = service.pickForDate(date);

        assertEquals(first, second);
    }

    @Test
    void pickForDate_variesOverMultipleDays() {
        var ayetTexts = new java.util.HashSet<String>();
        for (int i = 0; i < 30; i++) {
            ayetTexts.add(service.pickForDate(LocalDate.of(2026, 1, 1).plusDays(i)).ayet().text());
        }
        assertTrue(ayetTexts.size() > 1);
    }
}
