package com.aislam.rag.security;

import com.aislam.rag.exception.RagException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Client / admin secret karşılaştırması. */
public final class ApiSecretAuth {

    private ApiSecretAuth() {
    }

    public static void requireConfigured(boolean configured, String missingCode, String missingMessage) {
        if (!configured) {
            throw new RagException(missingMessage, missingCode);
        }
    }

    public static void requireMatch(
            String provided,
            String expected,
            String forbiddenCode,
            String forbiddenMessage
    ) {
        String p = provided == null ? "" : provided.trim();
        String e = expected == null ? "" : expected.trim();
        if (!constantTimeEquals(e, p)) {
            throw new RagException(forbiddenMessage, forbiddenCode);
        }
    }

    /** Authorization: Bearer xxx veya ham secret. */
    public static void requireAuthorizationMatch(
            String authorizationHeader,
            String expected,
            String forbiddenCode,
            String forbiddenMessage
    ) {
        String provided = stripBearerPrefix(authorizationHeader == null ? "" : authorizationHeader);
        String exp = stripBearerPrefix(expected == null ? "" : expected);
        requireMatch(provided, exp, forbiddenCode, forbiddenMessage);
    }

    public static boolean constantTimeEquals(String expected, String provided) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            MessageDigest.isEqual(a, a);
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }

    public static String stripBearerPrefix(String value) {
        String current = value.trim();
        while (true) {
            if (current.length() >= 7 && current.regionMatches(true, 0, "Bearer ", 0, 7)) {
                current = current.substring(7).trim();
                continue;
            }
            if (current.length() > 6
                    && current.regionMatches(true, 0, "Bearer", 0, 6)
                    && !Character.isWhitespace(current.charAt(6))) {
                current = current.substring(6).trim();
                continue;
            }
            break;
        }
        return current;
    }
}
