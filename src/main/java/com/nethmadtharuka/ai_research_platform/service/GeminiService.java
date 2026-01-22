package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.dto.GeminiRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.GeminiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String apiUrl;

    public GeminiService(
            WebClient webClient,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    public String generateContent(String prompt) {

        log.info("Calling Gemini API");

        GeminiRequest request = GeminiRequest.fromText(prompt);

        return webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)   // ✅ correct way
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Gemini API error: {}", body);
                                    return Mono.error(new RuntimeException("Gemini API error: " + body));
                                })
                )
                .bodyToMono(GeminiResponse.class)


                // ✅ NULL SAFETY
                .map(resp -> {
                    if (resp == null) {
                        return "Gemini returned empty response";
                    }
                    return resp.extractText();
                })

                // ✅ FINAL SAFETY NET (NO WHITELABEL)
                .onErrorResume(ex -> {
                    log.error("Gemini call failed", ex);
                    return Mono.just("⚠️ Gemini is currently unavailable.");
                })

                .block();
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
}
