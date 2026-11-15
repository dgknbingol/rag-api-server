package com.aislam.rag.exception;

import com.aislam.rag.dto.ApiErrorResponse;
import com.aislam.rag.util.Utf8Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
            case "DISPLAY_NAME_EXISTS" -> HttpStatus.CONFLICT;
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.UNAUTHORIZED;
        };
        return jsonError(status, ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(RagException.class)
    public ResponseEntity<ApiErrorResponse> handleRag(RagException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "LLM_ERROR", "DEEPSEEK_ERROR", "CONFIG_ERROR" -> HttpStatus.BAD_GATEWAY;
            case "QUIZ_PLAYER_NOT_FOUND", "QUIZ_QUESTION_NOT_FOUND", "QUIZ_OPTION_NOT_FOUND", "EDUCATION_TOPIC_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "QUIZ_ALREADY_ATTEMPTED", "EMAIL_EXISTS" -> HttpStatus.CONFLICT;
            case "QUIZ_VALIDATION_ERROR", "APP_USER_ID_REQUIRED", "APP_USER_ID_INVALID", "WEBHOOK_INVALID",
                 "PUSH_TARGET_INVALID" ->
                    HttpStatus.BAD_REQUEST;
            case "CHAT_QUOTA_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "CHAT_BUSY", "CHAT_QUEUE_FULL" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "CHAT_NOT_FOUND", "CHAT_CONVERSATION_NOT_FOUND", "CHAT_JOB_NOT_FOUND", "PUSH_DEVICE_NOT_FOUND" ->
                    HttpStatus.NOT_FOUND;
            case "WEBHOOK_UNAUTHORIZED", "PUSH_UNAUTHORIZED", "ADS_CONFIG_FORBIDDEN",
                 "DEVICE_TOKEN_REQUIRED", "DEVICE_TOKEN_INVALID", "DEVICE_TOKEN_MISMATCH", "AUTH_TOKEN_INVALID",
                 "PLAY_INTEGRITY_REQUIRED", "PLAY_INTEGRITY_HASH_MISMATCH", "PLAY_INTEGRITY_PACKAGE",
                 "PLAY_INTEGRITY_NOT_RECOGNIZED", "PLAY_INTEGRITY_DEVICE", "PLAY_INTEGRITY_INVALID",
                 "PLAY_INTEGRITY_DECODE_FAILED", "DEVICE_CHALLENGE_REQUIRED", "DEVICE_CHALLENGE_INVALID",
                 "DEVICE_CHALLENGE_MISMATCH" ->
                    HttpStatus.UNAUTHORIZED;
            case "DEVICE_ID_INVALID", "DEVICE_ID_REQUIRED", "CHAT_REWARD_SSV_REQUIRED",
                 "APP_ATTEST_REQUIRED", "APP_ATTEST_INVALID", "APP_ATTEST_UNKNOWN_KEY",
                 "APP_ATTEST_DEVICE_MISMATCH" -> HttpStatus.BAD_REQUEST;
            case "PLAY_INTEGRITY_NOT_CONFIGURED", "APP_ATTEST_NOT_CONFIGURED" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "PUSH_ADMIN_NOT_CONFIGURED", "ADS_CONFIG_ADMIN_DISABLED" ->
                    HttpStatus.SERVICE_UNAVAILABLE;
            case "ADS_CONFIG_WRITE" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return jsonError(status, ex.getMessage(), ex.getCode());
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
        return jsonError(HttpStatus.BAD_GATEWAY, message, "UPSTREAM_ERROR");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected error";
        return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, message, "INTERNAL_ERROR");
    }

    /**
     * SSE (text/event-stream) isteklerinde Content-Type preset kalırsa JSON body yazılamaz.
     * Hata yanıtlarını her zaman application/json olarak zorla.
     */
    private static ResponseEntity<ApiErrorResponse> jsonError(HttpStatus status, String message, String code) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiErrorResponse(message, code));
    }
}
