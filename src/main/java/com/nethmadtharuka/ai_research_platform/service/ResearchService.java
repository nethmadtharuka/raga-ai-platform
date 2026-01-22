package com.nethmadtharuka.ai_research_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchService {

    private final GeminiService geminiService;

    /**
     * Research a topic using AI
     */
    public String researchTopic(String topic) {
        log.info("Starting research on topic: {}", topic);

        String prompt = """
                You are a research assistant. Provide a comprehensive but concise overview of the following topic.
                
                Topic: %s
                
                Please include:
                1. A brief definition/explanation
                2. Key concepts or components
                3. Why it matters / real-world applications
                4. Current trends or recent developments
                
                Keep the response informative but concise (around 300-400 words).
                """.formatted(topic);

        return geminiService.generateContent(prompt);
    }

    /**
     * Summarize given content
     */
    public String summarize(String content) {
        log.info("Summarizing content of length: {} chars", content.length());

        String prompt = """
                Summarize the following content in a clear and concise manner.
                Highlight the key points and main takeaways.
                
                Content:
                %s
                
                Provide a summary in 3-5 bullet points.
                """.formatted(content);

        return geminiService.generateContent(prompt);
    }
}