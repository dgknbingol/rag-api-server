package com.aislam.rag.client;

import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.exception.RagException;

import java.util.List;

/**
 * Placeholder for a future cross-encoder reranker integration.
 * Not registered as a Spring bean while {@code rag.reranker-type=cross-encoder} remains disabled.
 */
public class CrossEncoderRerankerClient implements RerankerClient {

    @Override
    public List<SourceChunk> rerank(String question, List<SourceChunk> candidates, int limit) {
        throw new RagException("Cross-encoder reranker is not enabled yet. Set rag.reranker-type=keyword.");
    }
}
