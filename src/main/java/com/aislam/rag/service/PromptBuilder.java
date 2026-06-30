package com.aislam.rag.service;

import com.aislam.rag.domain.SourceChunk;

import java.util.List;

public interface PromptBuilder {

    String buildPrompt(String question, List<SourceChunk> sources);
}
