package com.example.sis.service.knowledge;

import com.example.sis.entity.KnowledgeDocument;
import com.example.sis.entity.KnowledgeEmbedding;
import com.example.sis.entity.MessageSource;
import com.example.sis.repository.KnowledgeDocumentRepository;
import com.example.sis.repository.KnowledgeEmbeddingRepository;
import com.example.sis.service.cohere.CohereService;
import com.example.sis.service.vector.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    
    private final CohereService cohereService;
    private final KnowledgeEmbeddingRepository embeddingRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final QdrantService qdrantService;
    
    private static final int CHUNK_SIZE = 500; // words per chunk
    private static final int CHUNK_OVERLAP = 50; // overlap between chunks
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final int MAX_RESULTS = 5;
    
    /**
     * Generate embeddings for a document and store in Qdrant (synchronous)
     * Chunks the document and creates embeddings for each chunk
     */
    @Transactional
    public void generateEmbeddingsForDocument(KnowledgeDocument document) {
        try {
            log.info("🔄 Generating embeddings for document: {} (docId: {})", document.getTitle(), document.getDocId());
            
            List<String> chunks = chunkText(document.getContent());
            log.info("📄 Split document {} into {} chunks", document.getDocId(), chunks.size());
            
            if (chunks.isEmpty()) {
                log.warn("⚠️ No chunks generated for document {}", document.getDocId());
                return;
            }
            
            List<QdrantService.QdrantPoint> qdrantPoints = new ArrayList<>();
            List<KnowledgeEmbedding> embeddingEntities = new ArrayList<>();
            
            for (int i = 0; i < chunks.size(); i++) {
                try {
                    String chunkText = chunks.get(i);
                    log.debug("🔍 Processing chunk {}/{}: {} chars", i + 1, chunks.size(), chunkText.length());
                    
                    List<Float> embedding = cohereService.createDocumentEmbedding(chunkText).block();
                    
                    if (embedding != null && !embedding.isEmpty()) {
                        // Generate Qdrant point ID (UUID for compatibility)
                        String pointId = UUID.randomUUID().toString();
                        
                        // Prepare Qdrant payload
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("doc_id", document.getDocId());
                        payload.put("doc_title", document.getTitle());
                        payload.put("chunk_index", i);
                        payload.put("chunk_text", chunkText);
                        payload.put("doc_type", document.getDocType());
                        
                        // Add academic context if exists
                        if (document.getRelatedEntityType() != null) {
                            payload.put("entity_type", document.getRelatedEntityType());
                            payload.put("entity_id", document.getRelatedEntityId());
                        }
                        
                        // Create Qdrant point
                        QdrantService.QdrantPoint point = new QdrantService.QdrantPoint(pointId, embedding, payload);
                        qdrantPoints.add(point);
                        
                        // Create MySQL metadata
                        KnowledgeEmbedding entity = new KnowledgeEmbedding();
                        entity.setDocument(document);
                        entity.setChunkIndex(i);
                        entity.setChunkText(chunkText);
                        entity.setQdrantPointId(pointId);
                        embeddingEntities.add(entity);
                        
                        log.debug("✅ Generated embedding for chunk {}/{} (vector size: {})", 
                            i + 1, chunks.size(), embedding.size());
                    } else {
                        log.warn("⚠️ Null or empty embedding returned for chunk {} of document {}", i, document.getDocId());
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to generate embedding for chunk {} of document {}: {}", 
                        i, document.getDocId(), e.getMessage(), e);
                }
            }
            
            // Batch upload to Qdrant
            if (!qdrantPoints.isEmpty()) {
                log.info("📤 Uploading {} points to Qdrant...", qdrantPoints.size());
                qdrantService.batchUpsertPoints(qdrantPoints);
                log.info("✅ Uploaded {} points to Qdrant successfully", qdrantPoints.size());
            } else {
                log.warn("⚠️ No Qdrant points to upload for document {}", document.getDocId());
            }
            
            // Save metadata to MySQL
            if (!embeddingEntities.isEmpty()) {
                log.info("💾 Saving {} embedding records to MySQL...", embeddingEntities.size());
                embeddingRepository.saveAll(embeddingEntities);
                log.info("✅ Saved {} embedding records to MySQL successfully", embeddingEntities.size());
            } else {
                log.warn("⚠️ No embedding entities to save for document {}", document.getDocId());
            }
            
            log.info("🎉 Completed embedding generation for document: {} ({} chunks processed)", 
                document.getDocId(), chunks.size());
                
        } catch (Exception e) {
            log.error("❌ CRITICAL: Failed to generate embeddings for document {}: {}", 
                document.getDocId(), e.getMessage(), e);
            // Don't rethrow - this is async, we just log the error
        }
    }
    
    /**
     * Search for relevant chunks using Qdrant vector similarity
     */
    public List<MessageSource> searchRelevantChunks(String query, Integer classId, Integer moduleId) {
        try {
            // Create query embedding
            List<Float> queryEmbedding = cohereService.createQueryEmbedding(query).block();
            if (queryEmbedding == null) {
                log.error("Failed to create query embedding");
                return List.of();
            }
            
            // Build filter for academic context
            Map<String, Object> filter = new HashMap<>();
            if (moduleId != null) {
                filter.put("entity_type", "MODULE");
                filter.put("entity_id", moduleId);
            } else if (classId != null) {
                filter.put("entity_type", "CLASS");
                filter.put("entity_id", classId);
            }
            
            // Search in Qdrant (now reactive)
            return qdrantService.searchSimilar(
                queryEmbedding, 
                MAX_RESULTS, 
                SIMILARITY_THRESHOLD,
                filter.isEmpty() ? null : filter
            )
            .doOnSuccess(sources -> 
                log.debug("Found {} relevant chunks for query (threshold: {})", sources.size(), SIMILARITY_THRESHOLD)
            )
            .block();
            
        } catch (Exception e) {
            log.error("Failed to search embeddings", e);
            return List.of();
        }
    }
    
    /**
     * Chunk text into smaller pieces with overlap
     */
    private List<String> chunkText(String text) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < words.length; i += CHUNK_SIZE - CHUNK_OVERLAP) {
            int end = Math.min(i + CHUNK_SIZE, words.length);
            String chunk = String.join(" ", Arrays.asList(words).subList(i, end));
            chunks.add(chunk);
            
            if (end >= words.length) break;
        }
        
        return chunks;
    }
}
