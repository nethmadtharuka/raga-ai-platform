package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final EmbeddingService embeddingService;

    // In-memory storage (replace with ChromaDB later)
    private final Map<String, Document> documents = new HashMap<>();

    /**
     * Add a document to the vector store
     */
    public Document addDocument(String title, String content, String source) {
        log.info("Adding document: {}", title);

        // Generate embedding for the content
        List<Double> embedding = embeddingService.generateEmbedding(content);

        // Create document
        Document doc = Document.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .content(content)
                .embedding(embedding)
                .createdAt(LocalDateTime.now())
                .source(source)
                .build();

        // Store it
        documents.put(doc.getId(), doc);
        log.info("Document added with ID: {}", doc.getId());

        return doc;
    }

    /**
     * Search for similar documents using semantic search
     */
    public List<Document> searchSimilar(String query, int topK) {
        log.info("Searching for: {}", query);

        if (documents.isEmpty()) {
            log.warn("No documents in store");
            return List.of();
        }

        // Generate embedding for query
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

        // Calculate similarity with all documents and sort
        List<Map.Entry<Document, Double>> scoredDocs = documents.values().stream()
                .map(doc -> Map.entry(doc, embeddingService.cosineSimilarity(queryEmbedding, doc.getEmbedding())))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .collect(Collectors.toList());

        // Log results
        scoredDocs.forEach(entry ->
                log.debug("Document: {} - Similarity: {}", entry.getKey().getTitle(), entry.getValue()));

        return scoredDocs.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get all documents
     */
    public List<Document> getAllDocuments() {
        return new ArrayList<>(documents.values());
    }

    /**
     * Get document by ID
     */
    public Optional<Document> getDocument(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    /**
     * Delete document
     */
    public boolean deleteDocument(String id) {
        return documents.remove(id) != null;
    }

    /**
     * Get document count
     */
    public int getDocumentCount() {
        return documents.size();
    }
}