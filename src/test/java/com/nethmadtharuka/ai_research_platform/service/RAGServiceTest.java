package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RAGServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private RAGService ragService;

    @Test
    void query_whenNoDocuments_returnsPromptToAddDocuments() {
        when(vectorStoreService.searchSimilar(anyString(), eq(3))).thenReturn(List.of());

        RAGService.RAGResponse response = ragService.query("What is RAG?");

        assertThat(response.getAnswer()).contains("don't have any documents");
        assertThat(response.getSourcesUsed()).isEmpty();
    }

    @Test
    void query_whenDocumentsExist_callsGeminiWithContext() {
        List<Document> docs = List.of(
                Document.builder()
                        .id("1")
                        .title("Doc A")
                        .content("Chunk text about Spring")
                        .source("s")
                        .createdAt(LocalDateTime.now())
                        .build());
        when(vectorStoreService.searchSimilar(eq("question?"), eq(3))).thenReturn(docs);
        when(geminiService.generateContent(org.mockito.ArgumentMatchers.contains("Chunk text about Spring")))
                .thenReturn("Answer from model");

        RAGService.RAGResponse response = ragService.query("question?");

        assertThat(response.getAnswer()).isEqualTo("Answer from model");
        assertThat(response.getSourcesUsed()).containsExactly("Doc A");
        assertThat(response.getDocumentsRetrieved()).isEqualTo(1);
        verify(geminiService).generateContent(org.mockito.ArgumentMatchers.contains("QUESTION: question?"));
    }

    @Test
    void query_whenGeminiFails_propagatesException() {
        List<Document> docs = List.of(
                Document.builder()
                        .id("1")
                        .title("T")
                        .content("C")
                        .createdAt(LocalDateTime.now())
                        .build());
        when(vectorStoreService.searchSimilar(anyString(), eq(3))).thenReturn(docs);
        when(geminiService.generateContent(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalStateException("Gemini down"));

        assertThatThrownBy(() -> ragService.query("q"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gemini down");
    }
}
