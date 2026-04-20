package com.nethmadtharuka.ai_research_platform.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 100_000)
    private String content;
}
