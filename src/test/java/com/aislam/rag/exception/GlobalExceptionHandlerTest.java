package com.aislam.rag.exception;

import com.aislam.rag.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void ragExceptionWithLlmErrorCodeReturnsStructuredResponse() {
        ResponseEntity<ApiErrorResponse> response = handler.handleRag(
                new RagException("LM Studio returned HTTP 400: model not found", "LLM_ERROR")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("LLM_ERROR", response.getBody().code());
        assertEquals("LM Studio returned HTTP 400: model not found", response.getBody().message());
    }
}
