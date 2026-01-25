package com.nethmadtharuka.ai_research_platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class N8nService {

    private final WebClient webClient;

    public N8nService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Trigger an n8n workflow via webhook
     */
    public Map<String, Object> triggerWorkflow(String webhookUrl, Map<String, Object> data) {
        log.info("Triggering n8n workflow: {}", webhookUrl);

        try {
            Map response = webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(data)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(30));

            log.info("Workflow triggered successfully");
            return response != null ? response : Map.of("status", "triggered");

        } catch (Exception e) {
            log.error("Error triggering workflow: {}", e.getMessage());
            throw new RuntimeException("Failed to trigger n8n workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Send data to n8n webhook with custom headers
     */
    public Map<String, Object> sendToWebhook(String webhookUrl, Map<String, Object> data,
                                             Map<String, String> headers) {
        log.info("Sending data to n8n webhook");

        try {
            WebClient.RequestHeadersSpec<?> request = webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(data);

            // Add custom headers if provided
            if (headers != null) {
                headers.forEach(request::header);
            }

            Map response = request
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(30));

            return response != null ? response : Map.of("status", "sent");

        } catch (Exception e) {
            log.error("Error sending to webhook: {}", e.getMessage());
            throw new RuntimeException("Failed to send to webhook: " + e.getMessage(), e);
        }
    }
}