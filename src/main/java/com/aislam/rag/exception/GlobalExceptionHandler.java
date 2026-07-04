package com.aislam.rag.exception;

import com.aislam.rag.dto.ApiErrorResponse;
import com.aislam.rag.util.Utf8Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("Validation Error");
        detail.setProperty("timestamp", Instant.now().toString());
        return detail;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AuthException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "EMAIL_EXISTS" -> HttpStatus.CONFLICT;
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.UNAUTHORIZED;
        };
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(ex.getMessage(), ex.getCode()));
    }

    @ExceptionHandler(RagException.class)
    public ResponseEntity<ApiErrorResponse> handleRag(RagException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "LLM_ERROR", "DEEPSEEK_ERROR", "CONFIG_ERROR" -> HttpStatus.BAD_GATEWAY;
            case "QUIZ_PLAYER_NOT_FOUND", "QUIZ_QUESTION_NOT_FOUND", "QUIZ_OPTION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "QUIZ_ALREADY_ATTEMPTED", "EMAIL_EXISTS" -> HttpStatus.CONFLICT;
            case "QUIZ_VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(ex.getMessage(), ex.getCode()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file exceeds maximum size of 50MB"
        );
        detail.setTitle("File Too Large");
        detail.setProperty("timestamp", Instant.now().toString());
        return detail;
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleWebClient(WebClientResponseException ex) {
        String message = "External service error: " + ex.getStatusCode() + " - "
                + Utf8Strings.fromBytes(ex.getResponseBodyAsByteArray());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(message, "UPSTREAM_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected error";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(message, "INTERNAL_ERROR"));
    }
}
