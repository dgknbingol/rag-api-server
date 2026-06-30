package com.aislam.rag.service;

import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.dto.SearchResultDto;

import java.util.List;

public interface RagService {

    AskResponse ask(String question);

    List<SourceChunk> retrieve(String question);

    List<SearchResultDto> retrieveSources(String question);
}
