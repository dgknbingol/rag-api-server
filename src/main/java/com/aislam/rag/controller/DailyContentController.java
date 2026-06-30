package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.DailyContentResponse;
import com.aislam.rag.service.DailyContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/daily", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class DailyContentController {

    private final DailyContentService dailyContentService;

    public DailyContentController(DailyContentService dailyContentService) {
        this.dailyContentService = dailyContentService;
    }

    @GetMapping("/today")
    public ResponseEntity<DailyContentResponse> today() {
        return ResponseEntity.ok(dailyContentService.getToday());
    }
}
