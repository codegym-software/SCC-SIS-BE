package com.example.sis.service.knowledge;

import com.example.sis.dto.knowledge.KnowledgeDocumentDTO;
import com.example.sis.dto.knowledge.KnowledgeDocumentRequest;
import com.example.sis.entity.KnowledgeDocument;
import com.example.sis.entity.KnowledgeEmbedding;
import com.example.sis.repository.KnowledgeDocumentRepository;
import com.example.sis.repository.KnowledgeEmbeddingRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.service.vector.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeService {
    
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeEmbeddingRepository embeddingRepository;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final UserRepository userRepository;
    
    /**
     * Create new knowledge document and generate embeddings
     */
    @Transactional
    public KnowledgeDocumentDTO createDocument(KnowledgeDocumentRequest request, Integer userId) {
        // TODO: Restore user lookup when User entity exists
        // User user = userRepository.findById(userId)
        //     .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create document entity
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setDocType(request.getDocType());
        document.setSourceUrl(request.getSourceUrl());
        document.setRelatedEntityType(request.getRelatedEntityType());
        document.setRelatedEntityId(request.getRelatedEntityId());
        document.setMetadata(request.getMetadata());
        document.setCreatedBy(userId); // Store user ID directly
        
        document = documentRepository.save(document);
        log.info("Created document: {} (docId: {})", document.getTitle(), document.getDocId());
        
        // Generate embeddings asynchronously
        embeddingService.generateEmbeddingsForDocument(document);
        
        return mapToDTO(document);
    }
    
    /**
     * Update existing document and regenerate embeddings
     */
    @Transactional
    public KnowledgeDocumentDTO updateDocument(Integer docId, KnowledgeDocumentRequest request) {
        KnowledgeDocument document = documentRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setDocType(request.getDocType());
        document.setSourceUrl(request.getSourceUrl());
        document.setRelatedEntityType(request.getRelatedEntityType());
        document.setRelatedEntityId(request.getRelatedEntityId());
        document.setMetadata(request.getMetadata());
        
        document = documentRepository.save(document);
        log.info("Updated document: {} (docId: {})", document.getTitle(), document.getDocId());
        
        // Delete old embeddings from Qdrant and MySQL
        qdrantService.deleteDocumentPoints(docId);
        embeddingRepository.deleteByDocument_DocId(docId);
        
        // Regenerate embeddings
        embeddingService.generateEmbeddingsForDocument(document);
        
        return mapToDTO(document);
    }
    
    /**
     * Delete document and its embeddings (from both Qdrant and MySQL)
     */
    @Transactional
    public void deleteDocument(Integer docId) {
        // Delete from Qdrant first
        qdrantService.deleteDocumentPoints(docId);
        
        // Delete from MySQL
        embeddingRepository.deleteByDocument_DocId(docId);
        documentRepository.deleteById(docId);
        
        log.info("Deleted document: {}", docId);
    }
    
    /**
     * Get all documents
     */
    public List<KnowledgeDocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Search documents by title/content
     */
    public List<KnowledgeDocumentDTO> searchDocuments(String query) {
        return documentRepository.searchByTitleOrContent(query).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get documents by type
     */
    public List<KnowledgeDocumentDTO> getDocumentsByType(String docType) {
        return documentRepository.findByDocTypeOrderByCreatedAtDesc(docType).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Delete ALL documents and clear Qdrant collection
     * WARNING: This will delete all knowledge documents and embeddings!
     */
    @Transactional
    public void deleteAllDocuments() {
        log.warn("⚠️ DELETING ALL KNOWLEDGE DOCUMENTS!");
        
        try {
            // Delete all embeddings from Qdrant
            qdrantService.deleteAllPoints();
            
            // Delete all embeddings from MySQL
            embeddingRepository.deleteAll();
            
            // Delete all documents from MySQL
            documentRepository.deleteAll();
            
            log.info("✅ Successfully deleted all knowledge documents and embeddings");
        } catch (Exception e) {
            log.error("❌ Failed to delete all documents", e);
            throw new RuntimeException("Failed to delete all documents", e);
        }
    }
    
    /**
     * Reindex all documents (regenerate embeddings in Qdrant)
     */
    @Transactional
    public void reindexAllDocuments() {
        log.info("Starting reindex of all documents...");
        List<KnowledgeDocument> documents = documentRepository.findAll();
        
        for (KnowledgeDocument document : documents) {
            try {
                // Delete old embeddings from Qdrant and MySQL
                qdrantService.deleteDocumentPoints(document.getDocId());
                embeddingRepository.deleteByDocument_DocId(document.getDocId());
                
                // Regenerate
                embeddingService.generateEmbeddingsForDocument(document);
                log.info("Reindexed document: {}", document.getDocId());
            } catch (Exception e) {
                log.error("Failed to reindex document: {}", document.getDocId(), e);
            }
        }
        
        log.info("Completed reindex of {} documents", documents.size());
    }
    
    private KnowledgeDocumentDTO mapToDTO(KnowledgeDocument document) {
        Long chunkCount = embeddingRepository.countByDocument_DocId(document.getDocId());
        
        return KnowledgeDocumentDTO.builder()
            .docId(document.getDocId())
            .title(document.getTitle())
            .content(document.getContent())
            .docType(document.getDocType())
            .sourceUrl(document.getSourceUrl())
            .relatedEntityType(document.getRelatedEntityType())
            .relatedEntityId(document.getRelatedEntityId())
            .metadata(document.getMetadata())
            .createdByName(document.getCreatedBy() != null ? "User #" + document.getCreatedBy() : null) // TODO: Restore when User entity exists
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .chunkCount(chunkCount.intValue())
            .build();
    }
    
    /**
     * Get user ID from email (for JWT token lookup)
     */
    public Integer getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(user -> user.getUserId())
            .orElse(null);
    }
}
