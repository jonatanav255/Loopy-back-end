package com.loopy.ai.service;

// Dependencies: @Service, @Value, RestClient — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Anthropic Messages API.
 * Uses Spring's RestClient to call Claude models directly.
 */
@Service
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AnthropicClient(@Value("${anthropic.api-key:}") String apiKey,
                           ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(API_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", API_VERSION)
                .build();
    }

    /**
     * Sends a message to Claude and returns the text response.
     *
     * @param model      model ID (e.g. "claude-haiku-4-5-20251001")
     * @param system     system prompt
     * @param userPrompt user message
     * @param maxTokens  max tokens in response
     * @return Claude's text response
     */
    public String sendMessage(String model, String system, String userPrompt, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key is not configured. Set ANTHROPIC_API_KEY environment variable.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        try {
            String responseJson = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode content = root.get("content");
            if (content != null && content.isArray() && !content.isEmpty()) {
                return content.get(0).get("text").asText();
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException("Anthropic API call failed: " + e.getMessage(), e);
        }
    }

    /** Returns true if the API key is configured. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
