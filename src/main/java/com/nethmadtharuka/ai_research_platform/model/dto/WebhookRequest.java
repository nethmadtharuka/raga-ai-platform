package com.nethmadtharuka.ai_research_platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {
    private String workflowId;

    @NotBlank(message = "Action is required")
    private String action;

    private Map<String, Object> data;
}