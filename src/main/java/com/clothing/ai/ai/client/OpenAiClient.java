package com.clothing.ai.ai.client;

import com.clothing.ai.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lightweight OpenAI HTTP client that mirrors the Spring AI ChatClient API surface
 * used throughout the codebase. Uses Spring's WebClient for non-blocking calls.
 *
 * If no API key is configured or the call fails, all methods return safe fallbacks
 * so the rest of the application keeps working.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient client() {
        String key = props.getAi().getOpenai().getApiKey();
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (key != null ? key : ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private boolean isConfigured() {
        String key = props.getAi().getOpenai().getApiKey();
        return props.getAi().isEnabled() && key != null && !key.isBlank();
    }

    /**
     * Simple chat completion. Returns plain text reply.
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, props.getAi().getOpenai().getModel());
    }

    /**
     * Chat with explicit model.
     */
    public String chat(String systemPrompt, String userPrompt, String model) {
        if (!isConfigured()) return null;
        try {
            ChatRequest req = new ChatRequest();
            req.setModel(model != null ? model : "gpt-4o-mini");
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank())
                messages.add(new Message("system", systemPrompt));
            messages.add(new Message("user", userPrompt));
            req.setMessages(messages);
            req.setTemperature(0.7);

            JsonNode resp = client().post()
                    .uri("/chat/completions")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(WebClientResponseException.class, e -> {
                        log.warn("OpenAI error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                        return Mono.empty();
                    })
                    .block(Duration.ofSeconds(30));
            if (resp == null) return null;
            JsonNode choices = resp.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) return null;
            return choices.get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("OpenAI chat failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Multi-turn chat with history.
     */
    public String chatWithHistory(List<Map<String,String>> history, String systemPrompt) {
        if (!isConfigured()) return null;
        try {
            ChatRequest req = new ChatRequest();
            req.setModel(props.getAi().getOpenai().getModel());
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null) messages.add(new Message("system", systemPrompt));
            for (var m : history) messages.add(new Message(m.get("role"), m.get("content")));
            req.setMessages(messages);
            req.setTemperature(0.7);

            JsonNode resp = client().post()
                    .uri("/chat/completions")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(WebClientResponseException.class, e -> Mono.empty())
                    .block(Duration.ofSeconds(30));
            if (resp == null) return null;
            return resp.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("OpenAI history chat failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate embeddings for semantic search / vector store.
     */
    public float[] embed(String input) {
        if (!isConfigured()) return null;
        try {
            EmbeddingRequest req = new EmbeddingRequest();
            req.setModel(props.getAi().getOpenai().getEmbeddingModel() != null
                    ? props.getAi().getOpenai().getEmbeddingModel() : "text-embedding-3-small");
            req.setInput(input);
            JsonNode resp = client().post()
                    .uri("/embeddings")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorResume(WebClientResponseException.class, e -> Mono.empty())
                    .block(Duration.ofSeconds(30));
            if (resp == null) return null;
            JsonNode arr = resp.path("data").path(0).path("embedding");
            if (!arr.isArray()) return null;
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        } catch (Exception e) {
            log.warn("Embedding failed: {}", e.getMessage());
            return null;
        }
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        public Message() {}
        public Message(String role, String content) { this.role = role; this.content = content; }
    }

    @Data
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;
    }

    @Data
    public static class EmbeddingRequest {
        private String model;
        private String input;
    }
}
