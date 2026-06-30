package com.aislam.rag.exception;

public class RagException extends RuntimeException {

    private final String code;

    public RagException(String message) {
        this(message, null, null);
    }

    public RagException(String message, String code) {
        this(message, code, null);
    }

    public RagException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public RagException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code != null ? code : "RAG_ERROR";
    }
}
