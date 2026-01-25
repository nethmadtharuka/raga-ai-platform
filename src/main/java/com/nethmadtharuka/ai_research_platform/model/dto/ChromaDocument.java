package com.nethmadtharuka.ai_research_platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChromaDocument {
    private List<String> ids;
    private List<List<Double>> embeddings;
    private List<String> documents;
    private List<Map<String, Object>> metadatas;
}