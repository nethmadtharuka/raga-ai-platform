package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VectorStoreService vectorStoreService;

    @MockitoBean
    private ResearchService researchService;

    @MockitoBean
    private RAGService ragService;

    @Test
    void testAddDocument_ValidInput_ReturnsSuccess() throws Exception {
        // Given
        Document doc = Document.builder()
                .id("test-id")
                .title("Test Document")
                .content("Test content")
                .createdAt(LocalDateTime.now())
                .build();

        when(vectorStoreService.addDocument(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(doc));

        // When & Then
        mockMvc.perform(post("/api/webhooks/add-document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Test content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document added successfully"));
    }

    @Test
    void testAddDocument_EmptyTitle_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/webhooks/add-document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"Test content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddDocument_EmptyContent_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/webhooks/add-document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAskWebhook_ValidQuestion_ReturnsSuccess() throws Exception {
        // Given
        RAGService.RAGResponse response = RAGService.RAGResponse.builder()
                .answer("Test answer")
                .sourcesUsed(Arrays.asList("source1"))
                .build();

        when(ragService.query(anyString())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/webhooks/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is Spring Boot?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testAskWebhook_EmptyQuestion_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/webhooks/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}