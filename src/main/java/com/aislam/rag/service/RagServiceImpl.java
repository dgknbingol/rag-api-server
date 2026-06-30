package com.aislam.rag.service;

import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.dto.SearchResultDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    private final AskOrchestratorService askOrchestratorService;
    private final RagRetrievalService ragRetrievalService;

    public RagServiceImpl(
            AskOrchestratorService askOrchestratorService,
            RagRetrievalService ragRetrievalService
    ) {
        this.askOrchestratorService = askOrchestratorService;
        this.ragRetrievalService = ragRetrievalService;
    }

    @Override
    public AskResponse ask(String question) {
        return askOrchestratorService.ask(question);
    }

    @Override
    public List<SourceChunk> retrieve(String question) {
        return ragRetrievalService.retrieve(question);
    }

    @Override
    public List<SearchResultDto> retrieveSources(String question) {
        return ragRetrievalService.retrieveSources(question);
    }
}
