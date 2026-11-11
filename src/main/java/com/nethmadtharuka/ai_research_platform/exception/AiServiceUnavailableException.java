package com.nethmadtharuka.ai_research_platform.exception;

/** The AI provider could not produce an answer (outage, bad key, unknown model). Mapped to HTTP 503. */
public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
