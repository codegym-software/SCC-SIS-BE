package com.example.sis.service.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Value("${openai.model:gpt-4o}")
    private String model;
    
    private WebClient getWebClient() {
        return webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    /**
     * Create embeddings for text using OpenAI API
     * @param text Text to embed
     * @return Embedding vector (1536 dimensions for text-embedding-3-small)
     */
    public Mono<List<Float>> createEmbedding(String text) {
        Map<String, Object> request = Map.of(
            "model", "text-embedding-3-small",
            "input", text
        );
        
        return getWebClient()
            .post()
            .uri("/embeddings")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                JsonNode embeddingNode = response.path("data").get(0).path("embedding");
                List<Float> embedding = objectMapper.convertValue(
                    embeddingNode, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Float.class)
                );
                log.debug("Created embedding with {} dimensions", embedding.size());
                return embedding;
            })
            .doOnError(e -> log.error("Failed to create embedding", e));
    }
    
    /**
     * Stream chat completion from OpenAI
     * @param messages Conversation history
     * @return Flux of text chunks
     */
    public Flux<String> streamChatCompletion(List<Map<String, String>> messages) {
        Map<String, Object> request = Map.of(
            "model", model,
            "messages", messages,
            "stream", true,
            "temperature", 0.7,
            "max_tokens", 1000
        );
        
        return getWebClient()
            .post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
            .map(line -> line.substring(6)) // Remove "data: " prefix
            .mapNotNull(json -> {
                try {
                    JsonNode node = objectMapper.readTree(json);
                    JsonNode delta = node.path("choices").get(0).path("delta").path("content");
                    return delta.isMissingNode() ? null : delta.asText();
                } catch (Exception e) {
                    log.error("Failed to parse SSE chunk: {}", json, e);
                    return null;
                }
            })
            .doOnError(e -> log.error("OpenAI streaming error", e));
    }
    
    /**
     * Get non-streaming chat completion with token usage
     */
    public Mono<ChatCompletionResult> getChatCompletion(List<Map<String, String>> messages) {
        Map<String, Object> request = Map.of(
            "model", model,
            "messages", messages,
            "temperature", 0.7,
            "max_tokens", 1000
        );
        
        return getWebClient()
            .post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                String content = response.path("choices").get(0).path("message").path("content").asText();
                int tokensUsed = response.path("usage").path("total_tokens").asInt();
                return new ChatCompletionResult(content, tokensUsed, model);
            })
            .doOnError(e -> log.error("Failed to get chat completion", e));
    }
    
    public record ChatCompletionResult(String content, int tokensUsed, String model) {}
}
