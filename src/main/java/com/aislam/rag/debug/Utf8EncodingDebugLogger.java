package com.aislam.rag.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Utf8EncodingDebugLogger {

    private static final Logger log = LoggerFactory.getLogger(Utf8EncodingDebugLogger.class);

    private Utf8EncodingDebugLogger() {
    }

    public static void logControllerQuestion(String question) {
        logLayer("1-CONTROLLER", "request-question", question);
    }

    public static void logPromptToLmStudio(String prompt) {
        logLayer("2-LM-STUDIO-PROMPT", "prompt", prompt);
    }

    public static void logLmStudioRawResponseBody(String rawJson) {
        logLayer("3-LM-STUDIO-RAW-BODY", "raw-response-body", rawJson);
    }

    public static void logFinalApiAnswer(String answer) {
        logLayer("4-API-FINAL-ANSWER", "final-answer", answer);
    }

    private static void logLayer(String layer, String label, String value) {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("[UTF8-DEBUG][{}] {} plain={}", layer, label, value);
        if (value == null) {
            log.debug("[UTF8-DEBUG][{}] {} base64=null", layer, label);
        } else {
            log.debug(
                    "[UTF8-DEBUG][{}] {} base64={}",
                    layer,
                    label,
                    Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))
            );
        }
    }
}
