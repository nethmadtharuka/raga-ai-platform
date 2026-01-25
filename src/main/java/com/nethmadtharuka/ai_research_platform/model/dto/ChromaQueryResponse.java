package com.nethmadtharuka.ai_research_platform.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ChromaQueryResponse {
    private List<List<String>> ids;
    private List<List<Double>> distances;
    private List<List<String>> documents;
    private List<List<Map<String, Object>>> metadatas;
}