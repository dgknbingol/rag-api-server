package com.aislam.rag.service;

import com.aislam.rag.domain.SourceChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagAskFlowLogger {

    private static final Logger log = LoggerFactory.getLogger(RagAskFlowLogger.class);
    private static final int PROMPT_PREVIEW_CHARS = 500;

    public void logRetrievedSources(String question, List<SourceChunk> rankedChunks) {
        log.info("Ask flow retrievedSourceCount={} question={}", sizeOf(rankedChunks), question);
        if (rankedChunks == null || rankedChunks.isEmpty()) {
            return;
        }
        for (int i = 0; i < rankedChunks.size(); i++) {
            SourceChunk chunk = rankedChunks.get(i);
            log.info(
                    "Ask flow rankedSource index={} documentId={} chunkIndex={} pageNumber={} "
                            + "sectionTitle={} vectorScore={} keywordScore={} phraseScore={} finalScore={}",
                    i,
                    chunk.documentId(),
                    chunk.chunkIndex(),
                    chunk.pageNumber(),
                    chunk.sectionTitle(),
                    chunk.vectorScore(),
                    chunk.keywordScore(),
                    chunk.phraseScore(),
                    chunk.score()
            );
        }
    }

    public void logPromptContext(List<SourceChunk> promptChunks, String prompt) {
        int contextChars = totalContentChars(promptChunks);
        log.info(
                "Ask flow promptSourceCount={} promptContextChars={} promptLength={}",
                sizeOf(promptChunks),
                contextChars,
                prompt != null ? prompt.length() : 0
        );
        if (prompt == null || prompt.isBlank()) {
            return;
        }
        String preview = prompt.length() <= PROMPT_PREVIEW_CHARS
                ? prompt
                : prompt.substring(0, PROMPT_PREVIEW_CHARS) + "...";
        log.info("Ask flow promptPreview={}", preview);
    }

    private static int totalContentChars(List<SourceChunk> chunks) {
        if (chunks == null) {
            return 0;
        }
        int total = 0;
        for (SourceChunk chunk : chunks) {
            if (chunk.content() != null) {
                total += chunk.content().length();
            }
        }
        return total;
    }

    private static int sizeOf(List<SourceChunk> chunks) {
        return chunks == null ? 0 : chunks.size();
    }
}
