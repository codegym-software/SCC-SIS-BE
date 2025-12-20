package com.example.sis.controller;

import com.example.sis.dto.knowledge.KnowledgeDocumentDTO;
import com.example.sis.dto.knowledge.KnowledgeDocumentRequest;
import com.example.sis.dto.knowledge.RAGTestResultDTO;
import com.example.sis.entity.MessageSource;
import com.example.sis.service.knowledge.DocumentTextExtractorService;
import com.example.sis.service.knowledge.EmbeddingService;
import com.example.sis.service.knowledge.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ADMIN')") // ✅ Only ADMIN can access
public class AdminKnowledgeController {
    
    private final KnowledgeService knowledgeService;
    private final EmbeddingService embeddingService;
    private final DocumentTextExtractorService textExtractorService;
    
    /**
     * Upload knowledge document file
     * POST /api/admin/knowledge/documents
     * 
     * Postman Setup:
     * - Body: form-data
     * - Key: file (Type: File) | Value: Select your PDF, DOC, DOCX, TXT or MD file
     * - Key: title (Type: Text) | Value: Document Title (optional)
     * - Key: docType (Type: Text) | Value: GUIDE (optional, default: GUIDE)
     * 
     * Supports: PDF, DOC, DOCX, TXT, MD (text-only files, no images)
     */
    @PostMapping("/documents")
    public KnowledgeDocumentDTO createDocument(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String docType
    ) throws IOException {
        // ✅ FIX: Get REAL userId from database using email from JWT
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found in JWT token");
        }
        
        // Query database to get actual user_id
        Integer userId = knowledgeService.getUserIdByEmail(email);
        if (userId == null) {
            throw new RuntimeException("User not found in database: " + email);
        }
        
        log.info("👑 Admin {} ({}) uploads document: {} ({} bytes)", 
            userId, email, file.getOriginalFilename(), file.getSize());
        
        try {
            // Extract text from file (PDF, DOC, DOCX, TXT, MD)
            String content = textExtractorService.extractText(file);
            
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("No text content found in file. File may contain only images.");
            }
            
            // Use filename as title if not provided
            String documentTitle = (title != null && !title.isEmpty()) 
                ? title 
                : file.getOriginalFilename();
            
            // Build request
            KnowledgeDocumentRequest request = new KnowledgeDocumentRequest();
            request.setTitle(documentTitle);
            request.setContent(content);
            request.setDocType(docType != null && !docType.isEmpty() ? docType : "GUIDE");
            
            log.info("📦 Creating document: title={}, docType={}, contentLength={}", 
                request.getTitle(), request.getDocType(), content.length());
            
            return knowledgeService.createDocument(request, userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to upload document: {}", e.getMessage(), e);
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update existing document
     */
    @PutMapping("/documents/{docId}")
    public KnowledgeDocumentDTO updateDocument(
        @PathVariable Integer docId,
        @RequestBody KnowledgeDocumentRequest request
    ) {
        log.info("Updating knowledge document: {}", docId);
        return knowledgeService.updateDocument(docId, request);
    }
    
    /**
     * Delete document
     */
    @DeleteMapping("/documents/{docId}")
    public void deleteDocument(@PathVariable Integer docId) {
        log.info("Deleting knowledge document: {}", docId);
        knowledgeService.deleteDocument(docId);
    }
    
    /**
     * Get all documents
     */
    @GetMapping("/documents")
    public List<KnowledgeDocumentDTO> getAllDocuments() {
        return knowledgeService.getAllDocuments();
    }
    
    /**
     * Get documents by type
     */
    @GetMapping("/documents/type/{docType}")
    public List<KnowledgeDocumentDTO> getDocumentsByType(@PathVariable String docType) {
        return knowledgeService.getDocumentsByType(docType);
    }
    
    /**
     * Search documents
     */
    @GetMapping("/documents/search")
    public List<KnowledgeDocumentDTO> searchDocuments(@RequestParam String query) {
        return knowledgeService.searchDocuments(query);
    }
    
    /**
     * Delete ALL documents and clear Qdrant collection
     * WARNING: This will delete all knowledge documents and embeddings!
     */
    @DeleteMapping("/documents/all")
    public void deleteAllDocuments() {
        log.warn("🗑️ DELETING ALL KNOWLEDGE DOCUMENTS - This cannot be undone!");
        knowledgeService.deleteAllDocuments();
    }
    
    /**
     * Reindex all documents (regenerate embeddings)
     */
    @PostMapping("/reindex")
    public void reindexAllDocuments() {
        log.info("Starting full reindex of knowledge base");
        knowledgeService.reindexAllDocuments();
    }
    
    /**
     * Test RAG retrieval
     */
    @GetMapping("/test-rag")
    public RAGTestResultDTO testRAG(
        @RequestParam String query,
        @RequestParam(required = false) Integer classId,
        @RequestParam(required = false) Integer moduleId
    ) {
        long startTime = System.currentTimeMillis();
        List<MessageSource> sources = embeddingService.searchRelevantChunks(query, classId, moduleId);
        int retrievalMs = (int) (System.currentTimeMillis() - startTime);
        
        return RAGTestResultDTO.builder()
            .query(query)
            .sources(sources)
            .retrievalMs(retrievalMs)
            .build();
    }
}
