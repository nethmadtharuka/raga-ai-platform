package com.nethmadtharuka.ai_research_platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nethmadtharuka.ai_research_platform.config.OpenApiConfig;
import com.nethmadtharuka.ai_research_platform.model.dto.request.AddDocumentRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.request.AskQuestionRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.request.ChunkingPreviewRequest;
import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.ChunkingService;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import jakarta.validation.Valid;
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
@Tag(name = "Documents", description = "Ingest and query documents (RAG)")
@SecurityRequirement(name = OpenApiConfig.API_KEY_HEADER)
public class DocumentController {

    private final VectorStoreService vectorStoreService;
    private final RAGService ragService;
    private final ChunkingService chunkingService;

    @PostMapping
    @Operation(summary = "Add a text document (chunked and embedded)")
    public ResponseEntity<List<Document>> addDocument(@Valid @RequestBody AddDocumentRequest request) {
        String source = resolveSource(request);
        List<Document> docs = vectorStoreService.addDocument(request.getTitle(), request.getContent(), source);
        return ResponseEntity.ok(docs);
    }

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

    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(vectorStoreService.searchSimilar(query, limit));
    }

    @PostMapping("/ask")
    public ResponseEntity<RAGService.RAGResponse> askQuestion(@Valid @RequestBody AskQuestionRequest request) {
        return ResponseEntity.ok(ragService.query(request.getQuestion()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(vectorStoreService.getStats());
    }

    @PostMapping("/chunking-preview")
    public ResponseEntity<ChunkingService.ChunkingMetadata> previewChunking(
            @Valid @RequestBody ChunkingPreviewRequest request) {
        return ResponseEntity.ok(chunkingService.getChunkingMetadata(request.getContent()));
    }

    private static String resolveSource(AddDocumentRequest request) {
        if (request.getMetadata() != null && request.getMetadata().containsKey("source")) {
            Object s = request.getMetadata().get("source");
            if (s instanceof String str) {
                return str;
            }
        }
        if (request.getSource() != null && !request.getSource().isBlank()) {
            return request.getSource();
        }
        return "manual";
    }
}
