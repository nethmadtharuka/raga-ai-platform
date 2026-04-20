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
public class AskQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 1000)
    private String question;
}
