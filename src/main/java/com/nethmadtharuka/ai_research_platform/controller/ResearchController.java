package com.nethmadtharuka.ai_research_platform.controller;

import com.nethmadtharuka.ai_research_platform.model.dto.request.SummarizeRequest;
import com.nethmadtharuka.ai_research_platform.service.ResearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ResearchController {

    private final ResearchService researchService;

    @GetMapping("/research/{topic}")
    public ResponseEntity<Map<String, String>> research(
            @PathVariable @NotBlank @Size(max = 200) String topic) {
        String result = researchService.researchTopic(topic);
        return ResponseEntity.ok(Map.of(
                "topic", topic,
                "research", result,
                "generatedAt", java.time.Instant.now().toString()
        ));
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarize(@Valid @RequestBody SummarizeRequest request) {
        String summary = researchService.summarize(request.getContent());
        return ResponseEntity.ok(Map.of(
                "summary", summary,
                "originalLength", String.valueOf(request.getContent().length()),
                "generatedAt", java.time.Instant.now().toString()
        ));
    }
}
