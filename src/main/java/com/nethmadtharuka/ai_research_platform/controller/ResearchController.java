package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchService researchService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI Research Platform",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/research/{topic}")
    public ResponseEntity<Map<String, String>> research(@PathVariable String topic) {
        String result = researchService.researchTopic(topic);
        return ResponseEntity.ok(Map.of(
                "topic", topic,
                "research", result,
                "generatedAt", java.time.Instant.now().toString()
        ));
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarize(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }

        String summary = researchService.summarize(content);
        return ResponseEntity.ok(Map.of(
                "summary", summary,
                "originalLength", String.valueOf(content.length()),
                "generatedAt", java.time.Instant.now().toString()
        ));
    }
}