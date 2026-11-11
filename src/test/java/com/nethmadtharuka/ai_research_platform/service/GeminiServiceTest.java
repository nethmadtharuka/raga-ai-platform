package com.nethmadtharuka.ai_research_platform.service;

import com.google.genai.errors.ApiException;
import com.nethmadtharuka.ai_research_platform.exception.AiServiceUnavailableException;
import com.nethmadtharuka.ai_research_platform.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceTest {

    private final List<String> calls = new ArrayList<>();

    private GeminiService service(GeminiService.ModelCall call) {
        return new GeminiService((m, p) -> {
            calls.add(m);
            return call.generate(m, p);
        }, "primary", "fallback", 1, 0);
    }

    @Test
    void quotaErrorSkipsStraightToFallbackModel() {
        String out = service((m, p) -> {
            if (m.equals("primary")) throw new ApiException(429, "RESOURCE_EXHAUSTED", "quota");
            return "from fallback";
        }).generateContent("q");

        assertThat(out).isEqualTo("from fallback");
        assertThat(calls).containsExactly("primary", "fallback"); // no retry against an exhausted quota
    }

    @Test
    void overloadIsRetriedThenFallsBack() {
        String out = service((m, p) -> {
            if (m.equals("primary")) throw new ApiException(503, "UNAVAILABLE", "overloaded");
            return "ok";
        }).generateContent("q");

        assertThat(out).isEqualTo("ok");
        assertThat(calls).containsExactly("primary", "primary", "fallback");
    }

    @Test
    void timeoutOnPrimaryFallsBackWithoutRetryingIt() {
        String out = service((m, p) -> {
            if (m.equals("primary")) throw new com.google.genai.errors.GenAiIOException("timed out", new java.net.SocketTimeoutException());
            return "from fallback";
        }).generateContent("q");

        assertThat(out).isEqualTo("from fallback");
        assertThat(calls).containsExactly("primary", "fallback");
    }

    @Test
    void deadlineExceededFallsBackWithoutRetrying() {
        String out = service((m, p) -> {
            if (m.equals("primary")) throw new ApiException(504, "DEADLINE_EXCEEDED", "slow");
            return "from fallback";
        }).generateContent("q");

        assertThat(out).isEqualTo("from fallback");
        assertThat(calls).containsExactly("primary", "fallback");
    }

    @Test
    void quotaOnEveryModelBecomesRateLimit() {
        GeminiService s = service((m, p) -> {
            throw new ApiException(429, "RESOURCE_EXHAUSTED", "quota");
        });
        assertThatThrownBy(() -> s.generateContent("q")).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void badKeyBecomesServiceUnavailable() {
        GeminiService s = service((m, p) -> {
            throw new ApiException(400, "INVALID_ARGUMENT", "API key not valid");
        });
        assertThatThrownBy(() -> s.generateContent("q")).isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void emptyTextIsAFailureNotAnAnswer() {
        assertThatThrownBy(() -> service((m, p) -> "  ").generateContent("q"))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
}
