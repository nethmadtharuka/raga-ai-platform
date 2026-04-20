package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import com.nethmadtharuka.ai_research_platform.service.RAGService;
import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import com.nethmadtharuka.ai_research_platform.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
@TestPropertySource(properties = {
        "app.api.key=test-api-key",
        "app.webhook.api-key=test-webhook-key",
        "gemini.api.key=dummy"
})
class WebhookControllerTest {

    private static final String API_KEY = "test-api-key";

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
        Document doc = Document.builder()
                .id("test-id")
                .title("Test Document")
                .content("Test content")
                .createdAt(LocalDateTime.now())
                .build();

        when(vectorStoreService.addDocument(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(doc));

        mockMvc.perform(post("/api/webhooks/add-document")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Test content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document added successfully"));
    }

    @Test
    void testAddDocument_EmptyTitle_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/webhooks/add-document")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"Test content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddDocument_EmptyContent_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/webhooks/add-document")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAskWebhook_ValidQuestion_ReturnsSuccess() throws Exception {
        RAGService.RAGResponse response = RAGService.RAGResponse.builder()
                .answer("Test answer")
                .sourcesUsed(Arrays.asList("source1"))
                .build();

        when(ragService.query(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/webhooks/ask")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is Spring Boot?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testAskWebhook_EmptyQuestion_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/webhooks/ask")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testWebhookWithoutApiKey_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/webhooks/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is Spring Boot?\"}"))
                .andExpect(status().isUnauthorized());
    }
}
