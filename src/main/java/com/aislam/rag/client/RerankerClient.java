package com.aislam.rag.client;

import com.aislam.rag.domain.SourceChunk;

import java.util.List;

public interface RerankerClient {

    List<SourceChunk> rerank(String question, List<SourceChunk> candidates, int limit);
}
