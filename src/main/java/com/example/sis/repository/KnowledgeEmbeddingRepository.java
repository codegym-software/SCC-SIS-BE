package com.example.sis.repository;

import com.example.sis.entity.KnowledgeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEmbeddingRepository extends JpaRepository<KnowledgeEmbedding, Integer> {
    
    // Delete all embeddings for a document (when reindexing)
    void deleteByDocument_DocId(Integer docId);
    
    // Count embeddings by document
    Long countByDocument_DocId(Integer docId);
    
    /**
     * Vector similarity search using PostgreSQL pgvector
     * Uses cosine similarity: 1 - (embedding <=> query_vector)
     * 
     * @param queryVector The query embedding as JSON array
     * @param limit Maximum number of results
     * @return List of embeddings ordered by similarity (highest first)
     */
    @Query(value = """
        SELECT 
            e.embedding_id,
            e.doc_id,
            e.chunk_index,
            e.chunk_text,
            e.embedding,
            e.created_at,
            1 - (e.embedding <=> CAST(:queryVector AS vector)) as similarity
        FROM knowledge_embeddings e
        WHERE 1 - (e.embedding <=> CAST(:queryVector AS vector)) > :threshold
        ORDER BY similarity DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> searchBySimilarity(
        @Param("queryVector") String queryVector,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );
    
    /**
     * Vector similarity search with context filter (class/module/lesson)
     * Only searches documents related to specific academic context
     */
    @Query(value = """
        SELECT 
            e.embedding_id,
            e.doc_id,
            e.chunk_index,
            e.chunk_text,
            e.embedding,
            e.created_at,
            1 - (e.embedding <=> CAST(:queryVector AS vector)) as similarity
        FROM knowledge_embeddings e
        JOIN knowledge_documents d ON e.doc_id = d.doc_id
        WHERE (
            (d.related_entity_type = :entityType AND d.related_entity_id = :entityId)
            OR d.related_entity_type IS NULL
        )
        AND 1 - (e.embedding <=> CAST(:queryVector AS vector)) > :threshold
        ORDER BY similarity DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> searchBySimilarityWithContext(
        @Param("queryVector") String queryVector,
        @Param("entityType") String entityType,
        @Param("entityId") Integer entityId,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );
}
