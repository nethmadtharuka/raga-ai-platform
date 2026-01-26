package com.nethmadtharuka.ai_research_platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChunkingService {

    @Value("${chunking.max-chunk-size:500}")
    private int maxChunkSize;

    @Value("${chunking.overlap-size:50}")
    private int overlapSize;

    /**
     * Split text into overlapping chunks
     * Strategy: Sentence-based chunking with overlap
     */
    public List<String> chunkText(String text) {

        if (text == null || text.isBlank()) {
            log.debug("Empty or null text provided for chunking");
            return List.of();
        }

        log.debug("Chunking text of length: {}", text.length());


        // Clean the text
        text = text.trim().replaceAll("\\s+", " ");

        // If text is small enough, return as single chunk
        if (text.length() <= maxChunkSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        List<String> sentences = splitIntoSentences(text);

        StringBuilder currentChunk = new StringBuilder();
        StringBuilder overlapBuffer = new StringBuilder();

        for (String sentence : sentences) {
            // If adding this sentence exceeds max size, save current chunk
            if (currentChunk.length() + sentence.length() > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // Start new chunk with overlap from previous chunk
                currentChunk = new StringBuilder(overlapBuffer.toString());
                overlapBuffer = new StringBuilder();
            }

            currentChunk.append(sentence).append(" ");

            // Build overlap buffer (last N characters of current chunk)
            String chunkSoFar = currentChunk.toString();
            if (chunkSoFar.length() > overlapSize) {
                overlapBuffer = new StringBuilder(
                        chunkSoFar.substring(chunkSoFar.length() - overlapSize)
                );
            } else {
                overlapBuffer = new StringBuilder(chunkSoFar);
            }
        }

        // Add the last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Split text into {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Split text into sentences
     * Handles common sentence endings: . ! ?
     */
    private List<String> splitIntoSentences(String text) {
        // Split on sentence endings followed by space and capital letter
        String[] rawSentences = text.split("(?<=[.!?])\\s+(?=[A-Z])");

        List<String> sentences = new ArrayList<>();
        for (String sentence : rawSentences) {
            if (!sentence.isBlank()) {
                sentences.add(sentence.trim());
            }
        }

        return sentences;
    }

    /**
     * Get metadata about chunking for a given text
     */
    public ChunkingMetadata getChunkingMetadata(String text) {
        List<String> chunks = chunkText(text);

        return ChunkingMetadata.builder()
                .totalChunks(chunks.size())
                .averageChunkSize(chunks.stream()
                        .mapToInt(String::length)
                        .average()
                        .orElse(0))
                .originalSize(text.length())
                .maxChunkSize(maxChunkSize)
                .overlapSize(overlapSize)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ChunkingMetadata {
        private int totalChunks;
        private double averageChunkSize;
        private int originalSize;
        private int maxChunkSize;
        private int overlapSize;
    }
}