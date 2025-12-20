package com.example.sis.service.cohere;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for Cohere AI API integration
 * Free tier: 1000 requests/month
 * Model: embed-english-v3.0 (1024 dimensions)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CohereService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${cohere.api.key}")
    private String apiKey;
    
    @Value("${cohere.api.base-url:https://api.cohere.ai/v1}")
    private String baseUrl;
    
    private WebClient getWebClient() {
        log.info("🔑 Using Cohere API key: {}...", apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) : "NULL");
        log.info("🌐 Using Cohere base URL: {}", baseUrl);
        
        // Configure HttpClient with timeouts
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // 60s connection timeout
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)) // 120s read timeout
                .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))) // 60s write timeout
            .responseTimeout(Duration.ofSeconds(120)); // 120s response timeout
        
        return webClientBuilder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    /**
     * Create embeddings for text using Cohere API
     * @param text Text to embed
     * @param inputType Type of input: "search_document" or "search_query"
     * @return Embedding vector (1024 dimensions for embed-english-v3.0)
     */
    public Mono<List<Float>> createEmbedding(String text, String inputType) {
        log.info("Creating Cohere embedding for text: {} chars, type: {}", text.length(), inputType);
        
        Map<String, Object> request = Map.of(
            "model", "embed-english-v3.0",
            "texts", List.of(text),
            "input_type", inputType,
            "embedding_types", List.of("float")
        );
        
        return getWebClient()
            .post()
            .uri("/embed")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(response -> {
                try {
                    // Extract embeddings array from response
                    @SuppressWarnings("unchecked")
                    Map<String, Object> embeddings = (Map<String, Object>) response.get("embeddings");
                    
                    @SuppressWarnings("unchecked")
                    List<List<Double>> floatEmbeddings = (List<List<Double>>) embeddings.get("float");
                    
                    // Get first embedding (we only sent one text)
                    List<Double> embedding = floatEmbeddings.get(0);
                    
                    log.info("✅ Generated Cohere embedding with {} dimensions", embedding.size());
                    
                    // Convert Double to Float
                    return embedding.stream()
                        .map(Double::floatValue)
                        .toList();
                        
                } catch (Exception e) {
                    log.error("Failed to parse Cohere response", e);
                    throw new RuntimeException("Failed to parse embedding response", e);
                }
            })
            .doOnError(error -> {
                log.error("❌ Failed to create Cohere embedding", error);
            });
    }
    
    /**
     * Create embedding for document storage
     */
    public Mono<List<Float>> createDocumentEmbedding(String text) {
        return createEmbedding(text, "search_document");
    }
    
    /**
     * Create embedding for search query
     */
    public Mono<List<Float>> createQueryEmbedding(String text) {
        return createEmbedding(text, "search_query");
    }
    
    /**
     * Generate text using Cohere Chat API v2 (non-streaming)
     * @param prompt The complete prompt to send to Cohere
     * @return Generated text response
     */
    public Mono<String> generateText(String prompt) {
        log.info("Generating text with Cohere, prompt length: {} chars", prompt.length());
        
        // Cohere Chat API v2 format
        Map<String, Object> message = Map.of(
            "role", "user",
            "content", prompt
        );
        
        Map<String, Object> request = Map.of(
            "model", "command-r-plus-08-2024",
            "messages", List.of(message),
            "temperature", 0.7,
            "max_tokens", 4000  // Increased to allow longer responses
        );
        
        return getWebClient()
            .post()
            .uri("/v2/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(response -> {
                try {
                    // Cohere Chat API v2 response: { "message": { "content": [{ "text": "..." }] } }
                    Map<String, Object> responseMessage = (Map<String, Object>) response.get("message");
                    List<Map<String, Object>> content = (List<Map<String, Object>>) responseMessage.get("content");
                    String text = (String) content.get(0).get("text");
                    
                    if (text == null || text.isEmpty()) {
                        throw new RuntimeException("Empty response from Cohere");
                    }
                    
                    log.info("✅ Generated text: {} chars", text.length());
                    return text;
                    
                } catch (Exception e) {
                    log.error("Failed to parse Cohere chat response v2", e);
                    throw new RuntimeException("Failed to parse chat response", e);
                }
            })
            .doOnError(error -> {
                log.error("❌ Failed to generate text with Cohere", error);
            });
    }
    
    /**
     * Generate text using Cohere Chat API with streaming (SSE)
     * @param prompt The complete prompt to send to Cohere
     * @return Flux of text chunks (Server-Sent Events)
     */
    public reactor.core.publisher.Flux<String> generateTextStream(String prompt) {
        log.info("🌊 Starting Cohere streaming, prompt length: {} chars", prompt.length());
        
        Map<String, Object> request = Map.of(
            "model", "command-r-plus",
            "message", prompt,
            "temperature", 0.7,
            "max_tokens", 2000,
            "stream", true  // ✅ Enable streaming
        );
        
        return getWebClient()
            .post()
            .uri("/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> !line.trim().isEmpty())
            .mapNotNull(line -> {
                try {
                    // Cohere sends JSON objects, not SSE format
                    // Parse each line as JSON
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = new com.fasterxml.jackson.databind.ObjectMapper().readValue(line, Map.class);
                    
                    // Check event type
                    String eventType = (String) json.get("event_type");
                    
                    if ("text-generation".equals(eventType)) {
                        // Extract text chunk
                        return (String) json.get("text");
                    } else if ("stream-end".equals(eventType)) {
                        // Stream finished
                        log.info("✅ Stream completed");
                        return null;
                    }
                    
                    return null;
                    
                } catch (Exception e) {
                    log.warn("Failed to parse streaming chunk: {}", line);
                    return null;
                }
            })
            .doOnNext(chunk -> log.debug("📤 Chunk: {} chars", chunk != null ? chunk.length() : 0))
            .doOnComplete(() -> log.info("✅ Streaming completed"))
            .doOnError(error -> log.error("❌ Streaming error", error));
    }
}
