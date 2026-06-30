package com.aislam.rag.dto;

import java.util.List;

public record AskResponse(
        String answer,
        List<SearchResultDto> sources
) {
}
