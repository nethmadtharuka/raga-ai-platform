package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.service.ChromaDBService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChromaDBService chromaDBService;

    @MockitoBean
    private VectorStoreService vectorStoreService;

    @Test
    void testHealth_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("AI Research Platform"));
    }

    @Test
    void testDetailedHealth_ChromaDBUp_ReturnsHealthy() throws Exception {
        // Given
        when(chromaDBService.getCollectionStats()).thenReturn(Map.of("count", 10));
        when(vectorStoreService.getStats()).thenReturn(Map.of("total_documents", 10));

        // When & Then
        mockMvc.perform(get("/api/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.chromadb.status").value("UP"));
    }

    @Test
    void testMetrics_ReturnsMetrics() throws Exception {
        // Given
        when(vectorStoreService.getStats()).thenReturn(Map.of("total_documents", 10));

        // When & Then
        mockMvc.perform(get("/api/health/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").exists())
                .andExpect(jsonPath("$.memory").exists());
    }
}