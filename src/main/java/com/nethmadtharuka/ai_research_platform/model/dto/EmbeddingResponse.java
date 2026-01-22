package com.nethmadtharuka.ai_research_platform.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class EmbeddingResponse {
    private Embedding embedding;

    @Data
    @NoArgsConstructor
    public static class Embedding {
        private List<Double> values;
    }

    public List<Double> getValues() {
        return embedding != null ? embedding.getValues() : List.of();
    }
}