package com.nethmadtharuka.ai_research_platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nethmadtharuka.ai_research_platform.config.OpenApiConfig;
import com.nethmadtharuka.ai_research_platform.model.dto.WebhookRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.WebhookResponse;
import com.nethmadtharuka.ai_research_platform.model.dto.request.WebhookAddDocumentRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.request.WebhookAskRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.request.WebhookResearchRequest;
import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import jakarta.validation.Valid;
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
@Tag(name = "Webhooks", description = "n8n-friendly endpoints (API key required)")
@SecurityRequirement(name = OpenApiConfig.API_KEY_HEADER)
public class WebhookController {

    private final VectorStoreService vectorStoreService;
    private final ResearchService researchService;
    private final RAGService ragService;

    @PostMapping("/n8n")
    public ResponseEntity<WebhookResponse> handleN8nWebhook(@Valid @RequestBody WebhookRequest request) {
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

    @PostMapping("/add-document")
    @Operation(summary = "Add document (webhook)")
    public ResponseEntity<WebhookResponse> addDocumentWebhook(@Valid @RequestBody WebhookAddDocumentRequest data) {
        log.info("Adding document via webhook: {}", data.getTitle());

        String source = resolveSource(data);

        List<Document> docs = vectorStoreService.addDocument(data.getTitle(), data.getContent(), source);

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

    @PostMapping("/research")
    public ResponseEntity<WebhookResponse> researchWebhook(@Valid @RequestBody WebhookResearchRequest data) {
        log.info("Research request via webhook");

        String research = researchService.researchTopic(data.getTopic());

        boolean shouldSave = Boolean.TRUE.equals(data.getSave());

        if (shouldSave) {
            vectorStoreService.addDocument(
                    "Research: " + data.getTopic(),
                    research,
                    "n8n_research"
            );
        }

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Research completed")
                .result(Map.of("topic", data.getTopic(), "research", research))
                .build());
    }

    @PostMapping("/ask")
    public ResponseEntity<WebhookResponse> askWebhook(@Valid @RequestBody WebhookAskRequest data) {
        log.info("Question via webhook");

        RAGService.RAGResponse response = ragService.query(data.getQuestion());

        return ResponseEntity.ok(WebhookResponse.builder()
                .success(true)
                .message("Question answered")
                .result(response)
                .build());
    }

    private static void validateWebhookRequest(WebhookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getAction() == null || request.getAction().isBlank()) {
            throw new IllegalArgumentException("Action is required");
        }
    }

    private static String resolveSource(WebhookAddDocumentRequest data) {
        if (data.getMetadata() != null && data.getMetadata().containsKey("source")) {
            Object s = data.getMetadata().get("source");
            if (s instanceof String str) {
                return str;
            }
        }
        if (data.getSource() != null && !data.getSource().isBlank()) {
            return data.getSource();
        }
        return "n8n_webhook";
    }

    private Object handleAddDocument(Map<String, Object> data) {
        String title = (String) data.get("title");
        String content = (String) data.get("content");
        String source = (String) data.getOrDefault("source", "n8n");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required and cannot be empty");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required and cannot be empty");
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
