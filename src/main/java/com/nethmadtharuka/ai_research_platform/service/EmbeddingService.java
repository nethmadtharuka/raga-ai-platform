package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.dto.EmbeddingRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class EmbeddingService {

    private final WebClient webClient;
    private final String apiKey;
    private final String embeddingUrl;
    /** The ask and search endpoints embed the same question; reuse it instead of a second request. */
    private final LruCache<String, List<Double>> queries = new LruCache<>(64);

    /** Embedding for a search query, cached by exact text. */
    public List<Double> embedQuery(String query) {
        List<Double> hit = queries.get(query);
        if (hit != null) return hit;
        List<Double> v = generateEmbedding(query);
        queries.put(query, v);
        return v;
    }

    public EmbeddingService(
            WebClient webClient,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.embedding.url}") String embeddingUrl) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.embeddingUrl = embeddingUrl;
    }

    /**
     * Generate embedding vector for given text
     */
    public List<Double> generateEmbedding(String text) {
        log.debug("Generating embedding for text: {}...",
                text.substring(0, Math.min(50, text.length())));

        EmbeddingRequest request = EmbeddingRequest.fromText(text);

        try {
            log.debug("Calling Gemini embedding API: {}", embeddingUrl);

            EmbeddingResponse response = webClient.post()
                    .uri(embeddingUrl)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    // Free tier: brief bursts of 429/503 while a long document is embedded piece by piece.
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(t -> t instanceof WebClientResponseException w
                                    && (w.getStatusCode().value() == 429 || w.getStatusCode().is5xxServerError()))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();

            if (response == null || response.getValues() == null) {
                log.error("Received null response from Gemini embedding API");
                throw new IllegalStateException("Null response from embedding service");
            }

            List<Double> values = response.getValues();
            log.debug("Generated embedding with {} dimensions", values.size());
            return values;

        } catch (WebClientResponseException e) {
            log.error("Gemini embedding API error: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 429) {
                throw new com.nethmadtharuka.ai_research_platform.exception.RateLimitExceededException("Embedding quota reached. Try again later.");
            }
            throw new com.nethmadtharuka.ai_research_platform.exception.AiServiceUnavailableException("Gemini embedding API error: " + e.getStatusCode(), e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            throw new com.nethmadtharuka.ai_research_platform.exception.AiServiceUnavailableException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Cosine similarity between two vectors (-1 to 1).
     */
    public double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += Math.pow(vec1.get(i), 2);
            norm2 += Math.pow(vec2.get(i), 2);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
