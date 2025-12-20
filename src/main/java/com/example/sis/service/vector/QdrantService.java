package com.example.sis.service.vector;

import com.example.sis.entity.MessageSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${qdrant.host:localhost}")
    private String qdrantHost;
    
    @Value("${qdrant.port:6333}")
    private int qdrantPort;
    
    @Value("${qdrant.collection.name:sis-knowledge-base}")
    private String collectionName;
    
    private static final int VECTOR_SIZE = 1024; // Cohere embed-english-v3.0
    
    private WebClient getWebClient() {
        return webClientBuilder
            .baseUrl(String.format("http://%s:%d", qdrantHost, qdrantPort))
            .build();
    }
    
    /**
     * Initialize Qdrant collection if not exists
     * Auto-called on application startup
     */
    @PostConstruct
    public void initializeCollection() {
        try {
            // Check if collection exists by trying to get its info
            Boolean exists = getWebClient()
                .get()
                .uri("/collections/{collection}", collectionName)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    // If we get a response body, collection exists
                    boolean hasResult = response != null && response.has("result");
                    log.debug("Collection check response: {}", hasResult);
                    return hasResult;
                })
                .onErrorResume(error -> {
                    // Any error (including 404) means collection doesn't exist
                    log.debug("Collection '{}' not found or error occurred: {}", collectionName, error.getMessage());
                    return Mono.just(false);
                })
                .block();
            
            if (Boolean.TRUE.equals(exists)) {
                log.info("Qdrant collection '{}' already exists", collectionName);
                return;
            }
            
            // Create collection
            Map<String, Object> createRequest = Map.of(
                "vectors", Map.of(
                    "size", VECTOR_SIZE,
                    "distance", "Cosine"
                )
            );
            
            getWebClient()
                .put()
                .uri("/collections/{collection}", collectionName)
                .bodyValue(createRequest)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.info("Created Qdrant collection: {}", collectionName);
            
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection", e);
            throw new RuntimeException("Qdrant initialization failed", e);
        }
    }
    
    /**
     * Upsert vector point to Qdrant
     */
    public void upsertPoint(String pointId, List<Float> vector, Map<String, Object> payload) {
        try {
            Map<String, Object> point = Map.of(
                "id", pointId,
                "vector", vector,
                "payload", payload
            );
            
            Map<String, Object> request = Map.of(
                "points", List.of(point)
            );
            
            getWebClient()
                .put()
                .uri("/collections/{collection}/points", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.debug("Upserted point {} to Qdrant", pointId);
            
        } catch (Exception e) {
            log.error("Failed to upsert point to Qdrant: {}", pointId, e);
            throw new RuntimeException("Qdrant upsert failed", e);
        }
    }
    
    /**
     * Batch upsert multiple points
     */
    public void batchUpsertPoints(List<QdrantPoint> points) {
        try {
            List<Map<String, Object>> pointMaps = points.stream()
                .map(point -> Map.of(
                    "id", (Object) point.getId(),
                    "vector", point.getVector(),
                    "payload", point.getPayload()
                ))
                .collect(Collectors.toList());
            
            Map<String, Object> request = Map.of("points", pointMaps);
            
            // Debug log
            log.info("🔍 Qdrant upsert request: {} points", points.size());
            if (!points.isEmpty()) {
                QdrantPoint firstPoint = points.get(0);
                log.info("   First point ID: {}", firstPoint.getId());
                log.info("   Vector size: {}", firstPoint.getVector().size());
                log.info("   Payload keys: {}", firstPoint.getPayload().keySet());
            }
            
            getWebClient()
                .put()
                .uri("/collections/{collection}/points", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.info("Batch upserted {} points to Qdrant", points.size());
            
        } catch (Exception e) {
            log.error("Failed to batch upsert points to Qdrant", e);
            throw new RuntimeException("Qdrant batch upsert failed", e);
        }
    }
    
    /**
     * Search for similar vectors (Reactive)
     */
    public Mono<List<MessageSource>> searchSimilar(List<Float> queryVector, int limit, Double threshold, Map<String, Object> filter) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("vector", queryVector);
            request.put("limit", limit);
            request.put("with_payload", true);
            
            if (threshold != null) {
                request.put("score_threshold", threshold);
            }
            
            if (filter != null && !filter.isEmpty()) {
                request.put("filter", Map.of("must", buildFilterConditions(filter)));
            }
            
            return getWebClient()
                .post()
                .uri("/collections/{collection}/points/search", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseSearchResults)
                .onErrorReturn(List.of());
            
        } catch (Exception e) {
            log.error("Failed to search in Qdrant", e);
            return Mono.just(List.of());
        }
    }
    
    /**
     * Delete point by ID
     */
    public void deletePoint(String pointId) {
        try {
            Map<String, Object> request = Map.of(
                "points", List.of(pointId)
            );
            
            getWebClient()
                .post()
                .uri("/collections/{collection}/points/delete", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.debug("Deleted point {} from Qdrant", pointId);
            
        } catch (Exception e) {
            log.error("Failed to delete point from Qdrant: {}", pointId, e);
        }
    }
    
    /**
     * Delete all points for a document
     */
    public void deleteDocumentPoints(Integer docId) {
        try {
            Map<String, Object> filter = Map.of(
                "must", List.of(
                    Map.of("key", "doc_id", "match", Map.of("value", docId))
                )
            );
            
            Map<String, Object> request = Map.of("filter", filter);
            
            getWebClient()
                .post()
                .uri("/collections/{collection}/points/delete", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.info("Deleted all points for document {} from Qdrant", docId);
            
        } catch (Exception e) {
            log.error("Failed to delete document points from Qdrant: {}", docId, e);
        }
    }
    
    /**
     * Delete ALL points from collection (clear entire collection)
     * WARNING: This will delete all vectors in the collection!
     */
    public void deleteAllPoints() {
        try {
            log.warn("⚠️ DELETING ALL POINTS from Qdrant collection: {}", collectionName);
            
            // Delete all points by deleting and recreating collection
            getWebClient()
                .delete()
                .uri("/collections/{collection}", collectionName)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            log.info("Deleted collection: {}", collectionName);
            
            // Recreate collection
            initializeCollection();
            
            log.info("✅ Successfully cleared all points from Qdrant");
            
        } catch (Exception e) {
            log.error("❌ Failed to delete all points from Qdrant", e);
            throw new RuntimeException("Failed to delete all points", e);
        }
    }
    
    /**
     * Get collection info
     */
    public Map<String, Object> getCollectionInfo() {
        try {
            JsonNode response = getWebClient()
                .get()
                .uri("/collections/{collection}", collectionName)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (response != null && response.has("result")) {
                return objectMapper.convertValue(response.get("result"), Map.class);
            }
            return Map.of();
            
        } catch (Exception e) {
            log.error("Failed to get collection info", e);
            return Map.of();
        }
    }
    
    // ===== Helper Methods =====
    
    private List<Map<String, Object>> buildFilterConditions(Map<String, Object> filter) {
        List<Map<String, Object>> conditions = new ArrayList<>();
        
        filter.forEach((key, value) -> {
            if (value != null) {
                conditions.add(Map.of(
                    "key", key,
                    "match", Map.of("value", value)
                ));
            }
        });
        
        return conditions;
    }
    
    private List<MessageSource> parseSearchResults(JsonNode response) {
        List<MessageSource> sources = new ArrayList<>();
        
        if (response == null || !response.has("result")) {
            return sources;
        }
        
        JsonNode results = response.get("result");
        for (JsonNode result : results) {
            try {
                MessageSource source = new MessageSource();
                
                double score = result.get("score").asDouble();
                source.setSimilarity(score);
                
                JsonNode payload = result.get("payload");
                source.setDocId(payload.get("doc_id").asInt());
                source.setChunkIndex(payload.get("chunk_index").asInt());
                source.setTitle(payload.has("doc_title") ? payload.get("doc_title").asText() : "Document #" + source.getDocId());
                
                String chunkText = payload.get("chunk_text").asText();
                // Use full chunk text for AI to ensure complete context (3000 chars covers typical chunk size)
                source.setExcerpt(chunkText.length() > 3000 ? chunkText.substring(0, 3000) + "..." : chunkText);
                
                sources.add(source);
                
            } catch (Exception e) {
                log.error("Failed to parse search result", e);
            }
        }
        
        return sources;
    }
    
    // ===== Data Classes =====
    
    public static class QdrantPoint {
        private String id;
        private List<Float> vector;
        private Map<String, Object> payload;
        
        public QdrantPoint(String id, List<Float> vector, Map<String, Object> payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }
        
        public String getId() { return id; }
        public List<Float> getVector() { return vector; }
        public Map<String, Object> getPayload() { return payload; }
    }
}
