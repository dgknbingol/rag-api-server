package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.MonthlyPrayerTimesResponse;
import com.aislam.rag.service.PrayerTimesService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/prayer-times", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
@Validated
public class PrayerTimesController {

    private final PrayerTimesService prayerTimesService;

    public PrayerTimesController(PrayerTimesService prayerTimesService) {
        this.prayerTimesService = prayerTimesService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyPrayerTimesResponse> monthly(
            @RequestParam @NotNull Double latitude,
            @RequestParam @NotNull Double longitude,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(prayerTimesService.getMonthly(latitude, longitude, year, month));
    }
}
