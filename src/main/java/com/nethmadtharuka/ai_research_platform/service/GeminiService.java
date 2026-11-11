package com.nethmadtharuka.ai_research_platform.service;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.nethmadtharuka.ai_research_platform.exception.AiServiceUnavailableException;
import com.nethmadtharuka.ai_research_platform.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Text generation through the official Google Gen AI SDK.
 *
 * Quota strategy (free tier):
 *  - 429 (quota) is never retried on the same model: it moves straight to the fallback model.
 *  - 500/502/503 (overloaded) are retried with a short backoff, then the fallback model is tried.
 *  - 504 and network timeouts go straight to the fallback model (the primary already used its full deadline).
 *  - Model names come from configuration (GEMINI_MODEL / GEMINI_FALLBACK_MODEL).
 */
@Service
@Slf4j
public class GeminiService {

    /** One request to one model; the seam lets tests fake the provider. */
    interface ModelCall {
        String generate(String model, String prompt);
    }

    private final ModelCall call;
    private final List<String> models;
    private final int retries;
    private final long backoffMs;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.fallback-model:}") String fallbackModel,
            @Value("${gemini.max-retries:1}") int retries,
            @Value("${gemini.retry-backoff-ms:1000}") long backoffMs,
            @Value("${gemini.timeout-ms:20000}") int timeoutMs) {
        this(sdkCall(apiKey, timeoutMs), model, fallbackModel, retries, backoffMs);
    }

    GeminiService(ModelCall call, String model, String fallbackModel, int retries, long backoffMs) {
        this.call = call;
        Set<String> chain = new LinkedHashSet<>();
        chain.add(model.trim());
        if (StringUtils.hasText(fallbackModel)) chain.add(fallbackModel.trim());
        this.models = List.copyOf(chain);
        this.retries = Math.max(0, retries);
        this.backoffMs = Math.max(0, backoffMs);
        log.info("Gemini models in use: {}", this.models);
    }

    public String generateContent(String prompt) {
        RuntimeException last = null;
        int lastCode = 0;
        for (String model : models) {
            for (int attempt = 0; attempt <= retries; attempt++) {
                try {
                    String text = call.generate(model, prompt);
                    if (StringUtils.hasText(text)) return text;
                    last = new AiServiceUnavailableException("Gemini " + model + " returned no text", null);
                    lastCode = 0;
                    log.warn("Gemini {} returned no text", model);
                    break; // next model
                } catch (ApiException e) {
                    last = e;
                    lastCode = e.code();
                    log.warn("Gemini {} failed (attempt {}): {} {}", model, attempt + 1, e.code(), e.status());
                    if (e.code() == 429 || !isTransient(e.code())) break; // next model
                    sleep(backoffMs * (attempt + 1));
                } catch (RuntimeException e) {
                    // Timeouts and network errors: an overloaded model often stalls, so move on to the fallback.
                    last = e;
                    lastCode = 0;
                    log.warn("Gemini {} failed (attempt {}): {}", model, attempt + 1, e.getClass().getSimpleName());
                    break; // next model
                }
            }
        }
        if (lastCode == 429) {
            throw new RateLimitExceededException("Gemini quota reached for " + models + ". Try again later.");
        }
        throw new AiServiceUnavailableException(
                "Gemini unavailable" + (lastCode != 0 ? " (" + lastCode + ")" : last != null ? " (" + last.getClass().getSimpleName() + ")" : ""), last);
    }

    public String generateWithContext(String systemPrompt, String userPrompt) {
        String combinedPrompt = """
                Instructions:
                %s

                User Request:
                %s
                """.formatted(systemPrompt, userPrompt);

        return generateContent(combinedPrompt);
    }

    private static ModelCall sdkCall(String apiKey, int timeoutMs) {
        Client client = Client.builder()
                .apiKey(StringUtils.hasText(apiKey) ? apiKey : "missing-key")
                .httpOptions(HttpOptions.builder()
                        .timeout(timeoutMs)
                        // Retries are handled in generateContent so quota errors are not retried on the same model.
                        .retryOptions(HttpRetryOptions.builder().attempts(1))
                        .build())
                .build();
        return (model, prompt) -> {
            GenerateContentResponse resp = client.models.generateContent(model, prompt, null);
            resp.usageMetadata().ifPresent(u -> log.debug("Gemini {} tokens: prompt={} total={}",
                    model, u.promptTokenCount().orElse(0), u.totalTokenCount().orElse(0)));
            return resp.text();
        };
    }

    /** Worth one quick retry on the same model. 504 is excluded: the model already took its full deadline, so fall back. */
    private static boolean isTransient(int code) {
        return code == 500 || code == 502 || code == 503;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
