package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.dto.ChromaDocument;
import com.nethmadtharuka.ai_research_platform.model.dto.ChromaQueryRequest;
import com.nethmadtharuka.ai_research_platform.model.dto.ChromaQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChromaDBService {

    private final WebClient webClient;
    private final String chromaUrl;
    private final String collectionName;
    private final String tenant = "default_tenant";
    private final String database = "default_database";
    private boolean initialized = false;
    private String collectionId = null;

    public ChromaDBService(
            WebClient webClient,
            @Value("${chroma.url}") String chromaUrl,
            @Value("${chroma.collection.name}") String collectionName) {
        this.webClient = webClient;
        this.chromaUrl = chromaUrl;
        this.collectionName = collectionName;
        log.info("ChromaDB Service initialized with URL: {}, Tenant: {}, Database: {}, Collection: {}",
                chromaUrl, tenant, database, collectionName);
    }

    private String getBaseCollectionUrl() {
        return chromaUrl + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections";
    }

    private void ensureInitialized() {
        if (!initialized) {
            initializeCollection();
            initialized = true;
        }
    }

    private void initializeCollection() {
        try {
            log.info("Initializing collection: {}", collectionName);

            // Try to get existing collection first
            try {
                List<Map> collections = webClient.get()
                        .uri(getBaseCollectionUrl())
                        .retrieve()
                        .bodyToMono(List.class)
                        .block(Duration.ofSeconds(10));

                if (collections != null) {
                    for (Map col : collections) {
                        if (collectionName.equals(col.get("name"))) {
                            collectionId = (String) col.get("id");
                            log.info("Found existing collection '{}' with ID: {}", collectionName, collectionId);
                            return;
                        }
                    }
                }
            } catch (WebClientResponseException.NotFound e) {
                log.info("No collections found, will create new one");
            }

            // Create new collection
            Map<String, Object> body = Map.of(
                    "name", collectionName,
                    "metadata", Map.of(
                            "description", "AI Research Platform documents",
                            "created_at", String.valueOf(System.currentTimeMillis())
                    )
            );

            Map response = webClient.post()
                    .uri(getBaseCollectionUrl())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (response != null && response.containsKey("id")) {
                collectionId = (String) response.get("id");
                log.info("Successfully created collection '{}' with ID: {}", collectionName, collectionId);
            }

        } catch (WebClientResponseException e) {
            log.error("HTTP error initializing collection: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to initialize ChromaDB collection: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error initializing collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize ChromaDB collection", e);
        }
    }

    public void addDocuments(List<String> ids, List<String> documents,
                             List<List<Double>> embeddings, List<Map<String, Object>> metadatas) {
        ensureInitialized();

        if (ids == null || ids.isEmpty()) {
            log.warn("No documents to add");
            return;
        }

        if (collectionId == null) {
            throw new RuntimeException("Collection ID is null - initialization failed");
        }

        log.info("Adding {} documents to ChromaDB collection ID: {}", ids.size(), collectionId);

        ChromaDocument chromaDoc = ChromaDocument.builder()
                .ids(ids)
                .documents(documents)
                .embeddings(embeddings)
                .metadatas(metadatas)
                .build();

        try {
            String url = getBaseCollectionUrl() + "/" + collectionId + "/add";
            log.debug("POST to: {}", url);

            Map response = webClient.post()
                    .uri(url)
                    .bodyValue(chromaDoc)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(30));

            log.info("Successfully added {} documents. Response: {}", ids.size(), response);

        } catch (WebClientResponseException e) {
            log.error("HTTP error adding documents: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to add documents: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error adding documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add documents: " + e.getMessage(), e);
        }
    }

    public ChromaQueryResponse query(List<Double> queryEmbedding, int nResults) {
        ensureInitialized();

        if (collectionId == null) {
            throw new RuntimeException("Collection ID is null - initialization failed");
        }

        log.debug("Querying ChromaDB collection ID: {} for {} results", collectionId, nResults);

        ChromaQueryRequest request = ChromaQueryRequest.builder()
                .query_embeddings(List.of(queryEmbedding))
                .n_results(nResults)
                .build();

        try {
            String url = getBaseCollectionUrl() + "/" + collectionId + "/query";

            ChromaQueryResponse response = webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChromaQueryResponse.class)
                    .block(Duration.ofSeconds(30));

            log.debug("Query returned {} results",
                    response != null && response.getDocuments() != null ?
                            response.getDocuments().size() : 0);
            return response;

        } catch (WebClientResponseException e) {
            log.error("HTTP error querying: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to query ChromaDB: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error querying ChromaDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query: " + e.getMessage(), e);
        }
    }

    // ✅ FIXED METHOD - This is the corrected version
    public Map<String, Object> getCollectionStats() {
        ensureInitialized();

        if (collectionId == null) {
            return Map.of("error", "Collection not initialized");
        }

        try {
            // Get collection count - ChromaDB returns a simple INTEGER, not a Map!
            Integer count = webClient.get()
                    .uri(getBaseCollectionUrl() + "/" + collectionId + "/count")
                    .retrieve()
                    .bodyToMono(Integer.class)  // ✅ Changed from Map.class to Integer.class
                    .block(Duration.ofSeconds(10));

            log.info("Collection count: {}", count);

            // Build the stats response
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalChunks", count != null ? count : 0);
            stats.put("totalDocuments", count != null ? (count / 3) : 0); // Rough estimate
            stats.put("uniqueSources", 0); // Can be enhanced later
            stats.put("collectionId", collectionId);
            stats.put("collectionName", collectionName);

            return stats;

        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage());
            return Map.of(
                    "totalChunks", 0,
                    "totalDocuments", 0,
                    "uniqueSources", 0,
                    "error", e.getMessage()
            );
        }
    }

    public void deleteCollection() {
        if (collectionId == null) {
            log.warn("Cannot delete - collection ID is null");
            return;
        }

        try {
            webClient.delete()
                    .uri(getBaseCollectionUrl() + "/" + collectionId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            initialized = false;
            collectionId = null;
            log.info("Deleted collection '{}'", collectionName);
        } catch (Exception e) {
            log.error("Error deleting collection: {}", e.getMessage());
        }
    }
}