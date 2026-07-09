package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.EducationCatalogResponse;
import com.aislam.rag.dto.EducationTopicDetailResponse;
import com.aislam.rag.service.EducationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/education", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class EducationController {

    private final EducationService educationService;

    public EducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<EducationCatalogResponse> catalog() {
        return ResponseEntity.ok(educationService.getCatalog());
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<EducationTopicDetailResponse> topic(@PathVariable String topicId) {
        return ResponseEntity.ok(educationService.getTopic(topicId));
    }
}
