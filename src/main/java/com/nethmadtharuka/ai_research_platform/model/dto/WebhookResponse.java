package com.nethmadtharuka.ai_research_platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    private boolean success;
    private String message;
    private Object result;
}