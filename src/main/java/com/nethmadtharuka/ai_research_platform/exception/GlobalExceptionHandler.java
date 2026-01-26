package com.nethmadtharuka.ai_research_platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input", e.getMessage());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientError(WebClientResponseException e) {
        log.error("External API error: {} - {}", e.getStatusCode(), e.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "External service error",
                "Failed to communicate with external service: " + e.getStatusCode()
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
        log.error("Resource not found: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception e) {
        log.error("Unexpected error", e);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again later."
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}