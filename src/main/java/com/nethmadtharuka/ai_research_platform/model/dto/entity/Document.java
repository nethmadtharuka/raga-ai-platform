package com.nethmadtharuka.ai_research_platform.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String id;
    private String title;
    private String content;
    private List<Double> embedding;
    private LocalDateTime createdAt;
    private String source;
}