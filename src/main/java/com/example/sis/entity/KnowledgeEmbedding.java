package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "knowledge_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeEmbedding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "embedding_id")
    private Integer embeddingId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private KnowledgeDocument document;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;
    
    // Qdrant point ID (vectors stored in Qdrant, not MySQL)
    @Column(name = "qdrant_point_id", nullable = false, unique = true)
    private String qdrantPointId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Transient field for similarity score (from search query)
    @Transient
    private Double similarity;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
