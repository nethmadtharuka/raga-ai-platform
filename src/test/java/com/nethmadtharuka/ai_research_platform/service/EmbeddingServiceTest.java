package com.nethmadtharuka.ai_research_platform.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    @Test
    void testCosineSimilarity_IdenticalVectors_ReturnsOne() {
        // Given
        EmbeddingService service = new EmbeddingService(null, null, null);
        List<Double> vec1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> vec2 = Arrays.asList(1.0, 2.0, 3.0);

        // When
        double similarity = service.cosineSimilarity(vec1, vec2);

        // Then
        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    void testCosineSimilarity_OrthogonalVectors_ReturnsZero() {
        // Given
        EmbeddingService service = new EmbeddingService(null, null, null);
        List<Double> vec1 = Arrays.asList(1.0, 0.0, 0.0);
        List<Double> vec2 = Arrays.asList(0.0, 1.0, 0.0);

        // When
        double similarity = service.cosineSimilarity(vec1, vec2);

        // Then
        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    void testCosineSimilarity_SimilarVectors_ReturnsHighSimilarity() {
        // Given
        EmbeddingService service = new EmbeddingService(null, null, null);
        List<Double> vec1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> vec2 = Arrays.asList(1.1, 2.1, 3.1);

        // When
        double similarity = service.cosineSimilarity(vec1, vec2);

        // Then
        assertTrue(similarity > 0.99, "Similar vectors should have high similarity");
    }

    @Test
    void testCosineSimilarity_DifferentSizeVectors_ThrowsException() {
        // Given
        EmbeddingService service = new EmbeddingService(null, null, null);
        List<Double> vec1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> vec2 = Arrays.asList(1.0, 2.0);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                service.cosineSimilarity(vec1, vec2)
        );
    }
}