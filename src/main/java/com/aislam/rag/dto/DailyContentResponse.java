package com.aislam.rag.dto;

public record DailyContentResponse(
        String date,
        DailyItemDto ayet,
        DailyItemDto dua,
        DailyItemDto hadis
) {
}
