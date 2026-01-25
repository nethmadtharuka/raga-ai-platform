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
     */
    @PostMapping
    public ResponseEntity<List<Document>> addDocument(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        String source = request.getOrDefault("source", "manual");

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
     * Get collection stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(vectorStoreService.getStats());
    }

    /**
     * Get chunking metadata for text
     */
    @PostMapping("/chunking-preview")
    public ResponseEntity<ChunkingService.ChunkingMetadata> previewChunking(
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chunkingService.getChunkingMetadata(content));
    }
}