package com.aislam.rag.service;

import com.aislam.rag.util.TurkishTextNormalizer;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Only exact short greetings — avoids treating real Islamic questions as small talk.
 */
@Component
public class ConversationalQueryDetector {

    private static final Set<String> EXACT_PHRASES = Set.of(
            "selam", "merhaba", "slm", "mrb", "hey", "hi", "hello",
            "selamun aleykum", "selamün aleyküm", "selamunaleykum", "selamünaleyküm",
            "aleykum selam", "aleyküm selam",
            "gunaydin", "günaydın", "iyi aksamlar", "iyi akşamlar", "iyi geceler",
            "nasilsin", "nasılsın", "naber", "ne haber",
            "tesekkurler", "teşekkürler", "sagol", "sağol", "eyvallah",
            "hosca kal", "hoşça kal", "gorusuruz", "görüşürüz", "gule gule", "güle güle"
    );

    public boolean isConversational(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = TurkishTextNormalizer.normalize(question).strip();
        return !normalized.isEmpty() && EXACT_PHRASES.contains(normalized);
    }

    public String respond(String question) {
        String normalized = TurkishTextNormalizer.normalize(question);

        if (normalized.contains("tesekkur") || normalized.contains("teşekkür")
                || normalized.equals("sagol") || normalized.equals("sağol") || normalized.equals("eyvallah")) {
            return "Rica ederim. İslami bir konuda sorunuz olursa yardımcı olmaya çalışırım.";
        }
        if (normalized.contains("hosca kal") || normalized.contains("hoşça kal")
                || normalized.contains("gorusuruz") || normalized.contains("görüşürüz")
                || normalized.contains("gule gule") || normalized.contains("güle güle")) {
            return "Allah'a emanet olun. İslami sorularınız için buradayım.";
        }
        if (normalized.contains("aleykum") || normalized.contains("aleyküm")) {
            return "Ve aleykümselam. AiSLAM'a hoş geldiniz; ilmihal kaynaklarına dayanarak İslami sorularınızı yanıtlamaya çalışırım.";
        }
        if (normalized.equals("nasilsin") || normalized.equals("nasılsın")
                || normalized.equals("naber") || normalized.equals("ne haber")) {
            return "Teşekkür ederim, iyiyim. Size İslami konularda nasıl yardımcı olabilirim?";
        }
        return "Aleykümselam. AiSLAM'a hoş geldiniz; ilmihal kaynaklarına dayanarak İslami sorularınızı yanıtlamaya çalışırım.";
    }
}
