package com.example.sis.repository;

import com.example.sis.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Integer> {
    
    // Find by doc type
    List<KnowledgeDocument> findByDocTypeOrderByCreatedAtDesc(String docType);
    
    // Find by related entity
    List<KnowledgeDocument> findByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
        String entityType, Integer entityId);
    
    // Full-text search by title or content
    @Query("SELECT d FROM KnowledgeDocument d WHERE " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<KnowledgeDocument> searchByTitleOrContent(@Param("query") String query);
    
    // Count documents by type
    Long countByDocType(String docType);
}
