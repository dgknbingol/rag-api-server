package com.aislam.rag.service;

import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.SourceChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilderImpl implements PromptBuilder {

    private static final String TRUNCATION_SUFFIX = "...";

    private final int maxAnswerWords;
    private final int maxSourceContentChars;
    private final int maxTotalSourcesChars;

    public PromptBuilderImpl(RagProperties properties) {
        this.maxAnswerWords = properties.maxAnswerWords();
        this.maxSourceContentChars = properties.maxSourceContentChars();
        this.maxTotalSourcesChars = properties.maxTotalSourcesChars();
    }

    @Override
    public String buildPrompt(String question, List<SourceChunk> sources) {
        return """
                Sen AiSLAM adlı İslami soru-cevap asistanısın.

                Kurallar:
                - Sadece verilen kaynaklara dayanarak cevap ver.
                - Kaynakta olmayan bilgi ekleme.
                - Tahmin yürütme.
                - Dini terim uydurma.
                - Kullanıcının sorduğu kavram ile kaynakta geçen kavram farklıysa bunu açıkça belirt.
                - Yakın görünen kavramları birbirinin yerine kullanma; örneğin "namazın farzları" ile "farz namazlar" aynı şey değildir.
                - Soruyu değiştirme veya farklı bir konuya cevap verme.
                - Cevap maksimum %d kelime olsun.
                - /no_think kullan.
                - Kaynaklarda cevap bulunamazsa: "Bu konuda yeterli kaynak bulunamadı." cevabını ver.

                Kullanıcı sorusu:
                %s

                Kaynaklar:
                %s

                Cevap:
                """.formatted(maxAnswerWords, question.trim(), buildSourcesBlock(sources));
    }

    private String buildSourcesBlock(List<SourceChunk> sources) {
        if (sources == null || sources.isEmpty()) {
            return "(Kaynak bulunamadı)";
        }

        StringBuilder sourcesBlock = new StringBuilder();
        int remainingContentBudget = maxTotalSourcesChars;

        for (int i = 0; i < sources.size(); i++) {
            if (remainingContentBudget <= 0) {
                break;
            }

            SourceChunk chunk = sources.get(i);
            sourcesBlock.append(i + 1).append(". ");
            if (chunk.title() != null && !chunk.title().isBlank()) {
                sourcesBlock.append("Başlık: ").append(chunk.title()).append("\n");
            }
            if (chunk.source() != null && !chunk.source().isBlank()) {
                sourcesBlock.append("Kaynak: ").append(chunk.source()).append("\n");
            }
            if (chunk.sectionTitle() != null && !chunk.sectionTitle().isBlank()) {
                sourcesBlock.append("Bölüm: ").append(chunk.sectionTitle()).append("\n");
            }
            if (chunk.pageNumber() != null) {
                sourcesBlock.append("Sayfa: ").append(chunk.pageNumber()).append("\n");
            }
            if (chunk.content() != null && !chunk.content().isBlank()) {
                int perSourceLimit = Math.min(maxSourceContentChars, remainingContentBudget);
                String content = truncateSafely(chunk.content(), perSourceLimit);
                sourcesBlock.append("İçerik: ").append(content).append("\n");
                remainingContentBudget -= content.length();
            }
            sourcesBlock.append("\n");
        }

        return sourcesBlock.toString().trim();
    }

    private String truncateSafely(String text, int maxChars) {
        if (text == null || maxChars <= 0) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= TRUNCATION_SUFFIX.length()) {
            return TRUNCATION_SUFFIX.substring(0, maxChars);
        }
        return text.substring(0, maxChars - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
    }
}
