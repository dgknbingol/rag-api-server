package com.aislam.rag.service;

import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.SourceChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderImplTest {

    private final PromptBuilderImpl promptBuilder = new PromptBuilderImpl(
            RagProperties.forTests()
    );

    @Test
    void buildPrompt_includesQuestionAndSources() {
        List<SourceChunk> sources = List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc-1", "İslam'ın Şartları", "İlmihal",
                        "İslam'ın beş şartı vardır.", "İslam", 12, 0, 0.92
                )
        );

        String prompt = promptBuilder.buildPrompt("İslam'ın şartı kaçtır?", sources);

        assertTrue(prompt.contains("İslam'ın şartı kaçtır?"));
        assertTrue(prompt.contains("Başlık: İslam'ın Şartları"));
        assertTrue(prompt.contains("Sayfa: 12"));
        assertTrue(prompt.contains("Bölüm: İslam"));
        assertTrue(prompt.contains("İçerik:"));
        assertTrue(prompt.contains("maksimum 80 kelime"));
        assertTrue(prompt.contains("namazın farzları"));
        assertTrue(prompt.contains("Soruyu değiştirme"));
    }

    @Test
    void truncatesLongSourceContentAndTotalBudget() {
        String longContent = "A".repeat(1500);
        List<SourceChunk> sources = List.of(
                SourceChunk.fromVectorSearch("1", "doc-1", "Kaynak 1", "Ilmihal", longContent, null, 0, 0.9),
                SourceChunk.fromVectorSearch("2", "doc-1", "Kaynak 2", "Ilmihal", longContent, null, 1, 0.8)
        );

        PromptBuilderImpl builder = new PromptBuilderImpl(
                RagProperties.forTests()
        );
        String prompt = builder.buildPrompt("Soru?", sources);

        assertTrue(prompt.contains("A".repeat(997) + "..."));
        assertTrue(prompt.indexOf("2. ") == -1 || prompt.length() < 6000);
    }
}
