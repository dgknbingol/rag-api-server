package com.aislam.rag.util;

import java.nio.charset.StandardCharsets;

public final class Utf8Strings {

    private Utf8Strings() {
    }

    public static String fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
