package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.ChunkingService;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final VectorStoreService vectorStoreService;
    private final RAGService ragService;
    private final ChunkingService chunkingService;

    /**
     * Add a document from text
     * FIXED: Changed from Map<String, String> to Map<String, Object> to handle nested metadata
     */
    @PostMapping
    public ResponseEntity<List<Document>> addDocument(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String content = (String) request.get("content");

        // Handle metadata - could be String or Map
        String source = "manual";
        if (request.containsKey("metadata")) {
            Object metadataObj = request.get("metadata");
            if (metadataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) metadataObj;
                source = (String) metadata.getOrDefault("source", "manual");
            } else if (metadataObj instanceof String) {
                source = (String) metadataObj;
            }
        } else if (request.containsKey("source")) {
            source = (String) request.get("source");
        }

        if (title == null || content == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Document> docs = vectorStoreService.addDocument(title, content, source);
        return ResponseEntity.ok(docs);
    }

    /**
     * Upload a file (TXT)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String content = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);

            List<Document> docs = vectorStoreService.addDocument(filename, content, "file_upload");

            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "filename", filename,
                    "chunks_created", docs.size(),
                    "documents", docs
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    /**
     * Search documents
     */
    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(vectorStoreService.searchSimilar(query, limit));
    }

    /**
     * Ask a question using RAG
     * FIXED: Changed from Map<String, String> to Map<String, Object>
     */
    @PostMapping("/ask")
    public ResponseEntity<RAGService.RAGResponse> askQuestion(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ragService.query(question));
    }

    /**
     * Get collection stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(vectorStoreService.getStats());
    }

    /**
     * Get chunking metadata for text
     * FIXED: Changed from Map<String, String> to Map<String, Object>
     */
    @PostMapping("/chunking-preview")
    public ResponseEntity<ChunkingService.ChunkingMetadata> previewChunking(
            @RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        if (content == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chunkingService.getChunkingMetadata(content));
    }
}