package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rag.reranking")
public record RerankingProperties(
        int minKeywordLength,
        double keywordFrequencyWeight,
        double keywordScoreMax,
        double phraseScorePerMatch,
        double phraseScoreMax,
        double sectionTitleKeywordWeight,
        double sectionTitleKeywordScoreMax,
        double sectionTitlePhraseScorePerMatch,
        double sectionTitlePhraseScoreMax,
        int minPhraseWords,
        int maxPhraseWords,
        double tocPenalty,
        double tocDotLeaderLineRatio,
        double numberDensityThreshold,
        double numberDensityPenalty,
        int pageReferenceThreshold,
        double pageReferencePenalty,
        double minLetterRatio,
        double lowSemanticTextPenalty,
        List<String> stopWords
) {
    public static RerankingProperties forTests() {
        return new RerankingProperties(
                3,
                0.03,
                0.15,
                0.12,
                0.30,
                0.08,
                0.25,
                0.20,
                0.40,
                2,
                4,
                0.20,
                0.35,
                0.25,
                0.15,
                3,
                0.10,
                0.55,
                0.12,
                defaultStopWords()
        );
    }

    public static List<String> defaultStopWords() {
        return List.of(
                "ve", "bir", "bu", "da", "de", "için", "ile", "mi", "mı", "mu", "mü",
                "nedir", "nelerdir", "nasıl", "ne", "kaç", "kim", "hangi", "olan", "olarak",
                "gibi", "kadar", "veya", "ama", "fakat", "the", "is", "are", "what", "how"
        );
    }
}
