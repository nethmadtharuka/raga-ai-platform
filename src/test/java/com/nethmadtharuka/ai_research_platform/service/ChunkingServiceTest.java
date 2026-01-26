package com.nethmadtharuka.ai_research_platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        // Set test values for chunk size and overlap
        ReflectionTestUtils.setField(chunkingService, "maxChunkSize", 100);
        ReflectionTestUtils.setField(chunkingService, "overlapSize", 20);
    }

    @Test
    void testChunkText_ShortText_ReturnsSingleChunk() {
        // Given
        String shortText = "This is a short text.";

        // When
        List<String> chunks = chunkingService.chunkText(shortText);

        // Then
        assertEquals(1, chunks.size());
        assertEquals(shortText, chunks.get(0));
    }

    @Test
    void testChunkText_LongText_ReturnsMultipleChunks() {
        // Given
        String longText = "This is a long text. It has multiple sentences. " +
                "Each sentence should be properly handled. " +
                "The chunking service should split this correctly. " +
                "We want to ensure overlapping works too.";

        // When
        List<String> chunks = chunkingService.chunkText(longText);

        // Then
        assertTrue(chunks.size() > 1, "Long text should produce multiple chunks");
        chunks.forEach(chunk ->
                assertTrue(chunk.length() <= 120, "Each chunk should be within size limit")
        );
    }

    @Test
    void testChunkText_NullInput_ReturnsEmptyList() {
        // When
        List<String> chunks = chunkingService.chunkText(null);

        // Then
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkText_EmptyInput_ReturnsEmptyList() {
        // When
        List<String> chunks = chunkingService.chunkText("   ");

        // Then
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testGetChunkingMetadata_ValidText_ReturnsCorrectMetadata() {
        // Given
        String text = "Test text for metadata calculation.";

        // When
        ChunkingService.ChunkingMetadata metadata = chunkingService.getChunkingMetadata(text);

        // Then
        assertNotNull(metadata);
        assertEquals(1, metadata.getTotalChunks());
        assertEquals(text.length(), metadata.getOriginalSize());
        assertEquals(100, metadata.getMaxChunkSize());
        assertEquals(20, metadata.getOverlapSize());
    }
}