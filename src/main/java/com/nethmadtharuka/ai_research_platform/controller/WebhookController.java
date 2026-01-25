package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.dto.WebhookRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.WebhookResponse;
import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Generic webhook endpoint for n8n
     */
    @PostMapping("/n8n")
    public ResponseEntity<WebhookResponse> handleN8nWebhook(@RequestBody WebhookRequest request) {
        log.info("Received n8n webhook: action={}", request.getAction());

        try {
            Object result = switch (request.getAction()) {
                case "add_document" -> handleAddDocument(request.getData());
                case "research_topic" -> handleResearchTopic(request.getData());
                case "ask_question" -> handleAskQuestion(request.getData());
                case "get_stats" -> handleGetStats();
                default -> Map.of("error", "Unknown action: " + request.getAction());
            };

            return ResponseEntity.ok(WebhookResponse.builder()
                    .success(true)
                    .message("Action completed successfully")
                    .result(result)
                    .build());

        } catch (Exception e) {
            log.error("Error handling webhook: {}", e.getMessage());
            return ResponseEntity.ok(WebhookResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .result(null)
                    .build());
        }
    }

    /**
     * Specific webhook for adding documents
     */
    @PostMapping("/add-document")
    public ResponseEntity<WebhookResponse> addDocumentWebhook(@RequestBody Map<String, String> data) {
        log.info("Adding document via webhook");

        String title = data.get("title");
        String content = data.get("content");
        String source = data.getOrDefault("source", "n8n_webhook");

        // Validate input
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(WebhookResponse.builder()
                    .success(false)
                    .message("Title is required and cannot be empty")
                    .build());
        }

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(WebhookResponse.builder()
                    .success(false)
                    .message("Content is required and cannot be empty")
                    .build());
        }

        List<Document> docs = vectorStoreService.addDocument(title, content, source);

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Document added successfully")
                .result(Map.of(
                        "chunks_created", docs.size(),
                        "document_ids", docs.stream().map(Document::getId).toList()
                ))
                .build());
    }

    /**
     * Specific webhook for research requests
     */
    @PostMapping("/research")
    public ResponseEntity<WebhookResponse> researchWebhook(@RequestBody Map<String, String> data) {
        log.info("Research request via webhook");

        String topic = data.get("topic");

        if (topic == null || topic.isBlank()) {
            return ResponseEntity.badRequest().body(WebhookResponse.builder()
                    .success(false)
                    .message("Topic is required")
                    .build());
        }

        String research = researchService.researchTopic(topic);

        // Optionally save to vector store
        if (Boolean.parseBoolean(data.getOrDefault("save", "false"))) {
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
     * Webhook for RAG queries
     */
    @PostMapping("/ask")
    public ResponseEntity<WebhookResponse> askWebhook(@RequestBody Map<String, String> data) {
        log.info("Question via webhook");

        String question = data.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(WebhookResponse.builder()
                    .success(false)
                    .message("Question is required")
                    .build());
        }

        RAGService.RAGResponse response = ragService.query(question);

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Question answered")
                .result(response)
                .build());
    }

    // Helper methods
    private Object handleAddDocument(Map<String, Object> data) {
        String title = (String) data.get("title");
        String content = (String) data.get("content");
        String source = (String) data.getOrDefault("source", "n8n");

        if (title == null || content == null || title.isBlank() || content.isBlank()) {
            throw new IllegalArgumentException("Title and content are required");
        }

        List<Document> docs = vectorStoreService.addDocument(title, content, source);
        return Map.of("chunks_created", docs.size());
    }

    private Object handleResearchTopic(Map<String, Object> data) {
        String topic = (String) data.get("topic");
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic is required");
        }
        return researchService.researchTopic(topic);
    }

    private Object handleAskQuestion(Map<String, Object> data) {
        String question = (String) data.get("question");
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question is required");
        }
        return ragService.query(question);
    }

    private Object handleGetStats() {
        return vectorStoreService.getStats();
    }
}