package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.dto.WebhookRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.WebhookResponse;
import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final VectorStoreService vectorStoreService;
    private final ResearchService researchService;
    private final RAGService ragService;

    private static final int MAX_CONTENT_LENGTH = 100000; // 100KB
    private static final int MAX_TITLE_LENGTH = 500;

    @PostMapping("/n8n")
    public ResponseEntity<WebhookResponse> handleN8nWebhook(@RequestBody WebhookRequest request) {
        log.info("Received n8n webhook: action={}", request.getAction());

        validateWebhookRequest(request);

        try {
            Object result = switch (request.getAction()) {
                case "add_document" -> handleAddDocument(request.getData());
                case "research_topic" -> handleResearchTopic(request.getData());
                case "ask_question" -> handleAskQuestion(request.getData());
                case "get_stats" -> handleGetStats();
                default -> throw new IllegalArgumentException("Unknown action: " + request.getAction());
            };

            return ResponseEntity.ok(WebhookResponse.builder()
                    .success(true)
                    .message("Action completed successfully")
                    .result(result)
                    .build());

        } catch (Exception e) {
            log.error("Error handling webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebhookResponse.builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }

    /**
     * FIXED: Changed from Map<String, String> to Map<String, Object>
     * This allows the endpoint to accept nested JSON objects like:
     * {
     *   "title": "...",
     *   "content": "...",
     *   "metadata": {
     *     "source": "frontend"
     *   }
     * }
     */
    @PostMapping("/add-document")
    public ResponseEntity<WebhookResponse> addDocumentWebhook(@RequestBody Map<String, Object> data) {
        log.info("Adding document via webhook: {}", data);

        // Extract and cast values properly
        String title = (String) data.get("title");
        String content = (String) data.get("content");

        // Handle source from either metadata object or direct field
        String source = "n8n_webhook";

        // Check if metadata exists and is a Map
        if (data.containsKey("metadata") && data.get("metadata") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            if (metadata.containsKey("source")) {
                source = (String) metadata.get("source");
            }
        } else if (data.containsKey("source")) {
            // Fallback to direct source field
            source = (String) data.get("source");
        }

        // Validation
        validateTitle(title);
        validateContent(content);

        List<Document> docs = vectorStoreService.addDocument(title, content, source);

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Document added successfully")
                .result(Map.of(
                        "chunks_created", docs.size(),
                        "document_ids", docs.stream().map(Document::getId).toList(),
                        "source", source
                ))
                .build());
    }

    /**
     * FIXED: Changed from Map<String, String> to Map<String, Object>
     */
    @PostMapping("/research")
    public ResponseEntity<WebhookResponse> researchWebhook(@RequestBody Map<String, Object> data) {
        log.info("Research request via webhook");

        String topic = (String) data.get("topic");
        validateTopic(topic);

        String research = researchService.researchTopic(topic);

        // Check if save flag is set
        Object saveObj = data.get("save");
        boolean shouldSave = false;
        if (saveObj instanceof Boolean) {
            shouldSave = (Boolean) saveObj;
        } else if (saveObj instanceof String) {
            shouldSave = Boolean.parseBoolean((String) saveObj);
        }

        if (shouldSave) {
            vectorStoreService.addDocument(
                    "Research: " + topic,
                    research,
                    "n8n_research"
            );
        }

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Research completed")
                .result(Map.of("topic", topic, "research", research))
                .build());
    }

    /**
     * FIXED: Changed from Map<String, String> to Map<String, Object>
     */
    @PostMapping("/ask")
    public ResponseEntity<WebhookResponse> askWebhook(@RequestBody Map<String, Object> data) {
        log.info("Question via webhook");

        String question = (String) data.get("question");
        validateQuestion(question);

        RAGService.RAGResponse response = ragService.query(question);

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Question answered")
                .result(response)
                .build());
    }

    // Validation methods
    private void validateWebhookRequest(WebhookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getAction() == null || request.getAction().isBlank()) {
            throw new IllegalArgumentException("Action is required");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required and cannot be empty");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title too long. Maximum length: " + MAX_TITLE_LENGTH);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required and cannot be empty");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content too long. Maximum length: " + MAX_CONTENT_LENGTH);
        }
    }

    private void validateTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic is required");
        }
        if (topic.length() > 200) {
            throw new IllegalArgumentException("Topic too long. Maximum length: 200");
        }
    }

    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question is required");
        }
        if (question.length() > 1000) {
            throw new IllegalArgumentException("Question too long. Maximum length: 1000");
        }
    }

    // Helper methods for /n8n endpoint
    private Object handleAddDocument(Map<String, Object> data) {
        String title = (String) data.get("title");
        String content = (String) data.get("content");
        String source = (String) data.getOrDefault("source", "n8n");

        validateTitle(title);
        validateContent(content);

        List<Document> docs = vectorStoreService.addDocument(title, content, source);
        return Map.of("chunks_created", docs.size());
    }

    private Object handleResearchTopic(Map<String, Object> data) {
        String topic = (String) data.get("topic");
        validateTopic(topic);
        return researchService.researchTopic(topic);
    }

    private Object handleAskQuestion(Map<String, Object> data) {
        String question = (String) data.get("question");
        validateQuestion(question);
        return ragService.query(question);
    }

    private Object handleGetStats() {
        return vectorStoreService.getStats();
    }
}