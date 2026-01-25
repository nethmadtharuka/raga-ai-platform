package com.nethmadtharuka.ai_research_platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChromaQueryRequest {
    private List<List<Double>> query_embeddings;
    private int n_results;
}