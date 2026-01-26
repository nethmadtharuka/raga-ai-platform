package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitService {

    private final Map<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    public void checkRateLimit(String identifier) {
        RateLimitInfo info = rateLimits.computeIfAbsent(identifier, k -> new RateLimitInfo());

        LocalDateTime now = LocalDateTime.now();

        // Reset if minute has passed
        if (now.isAfter(info.windowStart.plusMinutes(1))) {
            info.count = 0;
            info.windowStart = now;
        }

        info.count++;

        if (info.count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for: {}", identifier);
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Maximum " + MAX_REQUESTS_PER_MINUTE + " requests per minute."
            );
        }

        log.debug("Rate limit check: {} has {} requests", identifier, info.count);
    }

    private static class RateLimitInfo {
        int count = 0;
        LocalDateTime windowStart = LocalDateTime.now();
    }
}