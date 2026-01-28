package com.nethmadtharuka.ai_research_platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {
    private String model;  // ADD THIS LINE
    private Content content;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private Part[] parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    public static EmbeddingRequest fromText(String text) {
        return EmbeddingRequest.builder()
                .model("models/embedding-001")  // ADD THIS LINE
                .content(Content.builder()
                        .parts(new Part[]{Part.builder().text(text).build()})
                        .build())
                .build();
    }
}