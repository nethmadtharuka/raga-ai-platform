package com.nethmadtharuka.ai_research_platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.api.key:}")
    private String appApiKey;

    @Value("${app.webhook.api-key:}")
    private String webhookApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || isPublicApiPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = extractApiKey(request);
        if (!StringUtils.hasText(key)) {
            sendUnauthorized(response, "Missing API key");
            return;
        }

        if (path.startsWith("/api/webhooks/")) {
            if (isValidWebhookKey(key)) {
                setAuthenticated();
                filterChain.doFilter(request, response);
            } else {
                sendUnauthorized(response, "Invalid API key");
            }
            return;
        }

        if (isValidAppKey(key)) {
            setAuthenticated();
            filterChain.doFilter(request, response);
        } else {
            sendUnauthorized(response, "Invalid API key");
        }
    }

    private static boolean isPublicApiPath(String path) {
        return path.startsWith("/api/health");
    }

    private static void setAuthenticated() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "api",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    private String extractApiKey(HttpServletRequest request) {
        String header = request.getHeader("X-API-Key");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private boolean isValidAppKey(String key) {
        return StringUtils.hasText(appApiKey) && appApiKey.equals(key);
    }

    private boolean isValidWebhookKey(String key) {
        if (isValidAppKey(key)) {
            return true;
        }
        return StringUtils.hasText(webhookApiKey) && webhookApiKey.equals(key);
    }
}
