package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.service.ChromaDBService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final ChromaDBService chromaDBService;
    private final VectorStoreService vectorStoreService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "AI Research Platform");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());

        // Check ChromaDB
        try {
            Map<String, Object> chromaStats = chromaDBService.getCollectionStats();
            health.put("chromadb", Map.of("status", "UP", "stats", chromaStats));
        } catch (Exception e) {
            health.put("chromadb", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // Check Vector Store
        try {
            Map<String, Object> vectorStats = vectorStoreService.getStats();
            health.put("vector_store", Map.of("status", "UP", "stats", vectorStats));
        } catch (Exception e) {
            health.put("vector_store", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Map<String, Object> stats = vectorStoreService.getStats();
            metrics.put("documents", stats);
            metrics.put("timestamp", LocalDateTime.now());

            // Runtime metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("total_mb", runtime.totalMemory() / 1024 / 1024);
            memory.put("free_mb", runtime.freeMemory() / 1024 / 1024);
            memory.put("used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            metrics.put("memory", memory);

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}