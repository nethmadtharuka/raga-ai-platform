package com.nethmadtharuka.ai_research_platform.service;

import com.nethmadtharuka.ai_research_platform.model.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final VectorStoreService vectorStoreService;
    private final GeminiService geminiService;

    /**
     * Answer a question using RAG
     * 1. Find relevant documents
     * 2. Send them as context to LLM
     * 3. Get informed answer
     */
    public RAGResponse query(String question) {
        log.info("RAG Query: {}", question);

        // Step 1: Retrieve relevant documents
        List<Document> relevantDocs = vectorStoreService.searchSimilar(question, 3);

        if (relevantDocs.isEmpty()) {
            return RAGResponse.builder()
                    .answer("I don't have any documents to reference. Please add some documents first.")
                    .sourcesUsed(List.of())
                    .build();
        }

        // Step 2: Build context from documents
        String context = relevantDocs.stream()
                .map(doc -> "Title: " + doc.getTitle() + "\nContent: " + doc.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 3: Create prompt with context
        String prompt = """
                You are a helpful assistant. Answer the question based ONLY on the provided context.
                If the context doesn't contain enough information to answer, say so.
                
                CONTEXT:
                %s
                
                QUESTION: %s
                
                ANSWER:
                """.formatted(context, question);

        // Step 4: Get answer from LLM
        String answer = geminiService.generateContent(prompt);

        // Step 5: Return response with sources
        List<String> sources = relevantDocs.stream()
                .map(Document::getTitle)
                .collect(Collectors.toList());

        return RAGResponse.builder()
                .answer(answer)
                .sourcesUsed(sources)
                .documentsRetrieved(relevantDocs.size())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class RAGResponse {
        private String answer;
        private List<String> sourcesUsed;
        private int documentsRetrieved;
    }
}