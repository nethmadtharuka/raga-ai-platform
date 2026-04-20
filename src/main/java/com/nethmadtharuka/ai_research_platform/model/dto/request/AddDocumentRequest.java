package com.nethmadtharuka.ai_research_platform.model.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddDocumentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 100_000)
    private String content;

    private String source;

    private Map<String, Object> metadata;
}
