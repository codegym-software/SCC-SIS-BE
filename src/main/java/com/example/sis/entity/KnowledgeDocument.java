package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "knowledge_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Integer docId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;
    
    @Column(name = "source_url", length = 500)
    private String sourceUrl;
    
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;
    
    @Column(name = "related_entity_id")
    private Integer relatedEntityId;
    
    // Metadata (author, version, tags, etc.)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;
    
    // TODO: Restore when User entity exists
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "created_by")
    // private User createdBy;
    
    @Column(name = "created_by")
    private Integer createdBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KnowledgeEmbedding> embeddings;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
