package com.aislam.rag.debug;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Utf8EncodingDebugLoggerTest {

    @Test
    void base64RoundTripForTurkishText() {
        String plain = "Müslüman olmak için İslam";
        String base64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
        String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        assertEquals(plain, decoded);
    }

    @Test
    void logMethodsDoNotThrow() {
        assertDoesNotThrow(() -> {
            Utf8EncodingDebugLogger.logControllerQuestion("Müslüman İslam Şehadet oruç");
            Utf8EncodingDebugLogger.logPromptToLmStudio("oruç ve Şehadet");
            Utf8EncodingDebugLogger.logLmStudioRawResponseBody("{\"content\":\"İslam\"}");
            Utf8EncodingDebugLogger.logFinalApiAnswer("Müslüman");
            Utf8EncodingDebugLogger.logControllerQuestion(null);
        });
    }
}
