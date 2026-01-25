package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.dto.ChromaQueryResponse;
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
    private final ChromaDBService chromaDBService;
    private final ChunkingService chunkingService;

    /**
     * Add a document with automatic chunking
     */
    public List<Document> addDocument(String title, String content, String source) {
        log.info("Adding document: {} ({} chars)", title, content.length());

        // Step 1: Chunk the document
        List<String> chunks = chunkingService.chunkText(content);
        log.info("Document split into {} chunks", chunks.size());

        // Step 2: Generate embeddings for each chunk
        List<String> ids = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<List<Double>> embeddings = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();
        List<Document> savedDocs = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = UUID.randomUUID().toString();

            // Generate embedding
            List<Double> embedding = embeddingService.generateEmbedding(chunk);

            // Prepare metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title);
            metadata.put("source", source);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            metadata.put("created_at", LocalDateTime.now().toString());

            ids.add(chunkId);
            documents.add(chunk);
            embeddings.add(embedding);
            metadatas.add(metadata);

            // Create Document object for return
            Document doc = Document.builder()
                    .id(chunkId)
                    .title(title + " (chunk " + (i + 1) + "/" + chunks.size() + ")")
                    .content(chunk)
                    .embedding(embedding)
                    .createdAt(LocalDateTime.now())
                    .source(source)
                    .build();
            savedDocs.add(doc);
        }

        // Step 3: Add to ChromaDB
        chromaDBService.addDocuments(ids, documents, embeddings, metadatas);

        log.info("Added {} chunks to ChromaDB", chunks.size());
        return savedDocs;
    }

    /**
     * Search for similar documents
     */
    public List<Document> searchSimilar(String query, int topK) {
        log.info("Searching for: {}", query);

        // Generate embedding for query
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

        // Query ChromaDB
        ChromaQueryResponse response = chromaDBService.query(queryEmbedding, topK);

        // Convert to Document objects
        List<Document> results = new ArrayList<>();

        if (response.getDocuments() != null && !response.getDocuments().isEmpty()) {
            List<String> ids = response.getIds().get(0);
            List<String> docs = response.getDocuments().get(0);
            List<Map<String, Object>> metas = response.getMetadatas().get(0);
            List<Double> distances = response.getDistances().get(0);

            for (int i = 0; i < ids.size(); i++) {
                Map<String, Object> meta = metas.get(i);

                Document doc = Document.builder()
                        .id(ids.get(i))
                        .title((String) meta.get("title"))
                        .content(docs.get(i))
                        .source((String) meta.get("source"))
                        .createdAt(LocalDateTime.parse((String) meta.get("created_at")))
                        .build();

                results.add(doc);

                log.debug("Result {}: {} (distance: {})",
                        i + 1, meta.get("title"), distances.get(i));
            }
        }

        return results;
    }

    /**
     * Get collection stats
     */
    public Map<String, Object> getStats() {
        return chromaDBService.getCollectionStats();
    }
}