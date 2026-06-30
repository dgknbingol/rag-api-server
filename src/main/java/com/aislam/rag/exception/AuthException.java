package com.aislam.rag.exception;

public class AuthException extends RuntimeException {

    private final String code;

    public AuthException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
