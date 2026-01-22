package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final VectorStoreService vectorStoreService;
    private final RAGService ragService;

    /**
     * Add a new document to the knowledge base
     */
    @PostMapping
    public ResponseEntity<Document> addDocument(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        String source = request.getOrDefault("source", "manual");

        if (title == null || content == null) {
            return ResponseEntity.badRequest().build();
        }

        Document doc = vectorStoreService.addDocument(title, content, source);
        return ResponseEntity.ok(doc);
    }

    /**
     * Get all documents
     */
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(vectorStoreService.getAllDocuments());
    }

    /**
     * Search documents semantically
     */
    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(vectorStoreService.searchSimilar(query, limit));
    }

    /**
     * Ask a question using RAG
     */
    @PostMapping("/ask")
    public ResponseEntity<RAGService.RAGResponse> askQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ragService.query(question));
    }

    /**
     * Get document count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getDocumentCount() {
        return ResponseEntity.ok(Map.of("count", vectorStoreService.getDocumentCount()));
    }

    /**
     * Delete a document
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteDocument(@PathVariable String id) {
        boolean deleted = vectorStoreService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}