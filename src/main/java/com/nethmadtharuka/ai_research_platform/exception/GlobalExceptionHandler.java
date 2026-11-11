package com.nethmadtharuka.ai_research_platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${app.errors.debug:false}")
    private boolean debug;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input", e.getMessage(), e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Request validation failed: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", message, e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", message, e);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientError(WebClientResponseException e) {
        log.error("External API error: {} - {}", e.getStatusCode(), e.getMessage());
        // If Gemini returns 401/403/429 etc, keep the real status
        return buildErrorResponse(
                HttpStatus.valueOf(e.getStatusCode().value()),
                "External service error",
                e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isBlank()
                        ? e.getResponseBodyAsString()
                        : ("Failed to communicate with external service: " + e.getStatusCode()),
                e
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", e.getMessage(), e);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAiUnavailable(AiServiceUnavailableException e) {
        log.error("AI service unavailable: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "External service error",
                "The answering service is unavailable. Please try again later.", e);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
        log.error("Resource not found: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage(), e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception e) {
        log.error("Unexpected error", e);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again later.",
                e
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message, Exception e) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);

        // ✅ only include these when debugging
        if (debug) {
            response.put("exception", e.getClass().getName());
            response.put("details", e.getMessage());
        }

        return ResponseEntity.status(status).body(response);
    }
}
