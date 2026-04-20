package com.nethmadtharuka.ai_research_platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String API_KEY_HEADER = "X-API-Key";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Research Platform API")
                        .description("RAG pipeline with ChromaDB and Google Gemini")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_HEADER))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_HEADER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(API_KEY_HEADER)));
    }
}
