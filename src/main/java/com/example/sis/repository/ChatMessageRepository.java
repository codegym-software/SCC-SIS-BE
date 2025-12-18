package com.example.sis.repository;

import com.example.sis.entity.ChatMessage;
import com.example.sis.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    
    List<ChatMessage> findBySessionOrderByCreatedAtDesc(ChatSession session);
    
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
    
    // ===== Analytics Queries =====
    
    /**
     * Count messages after a certain date
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.createdAt >= :startDate")
    Long countMessagesAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Sum tokens used after a certain date
     */
    @Query("SELECT COALESCE(SUM(m.tokensUsed), 0L) FROM ChatMessage m WHERE m.createdAt >= :startDate AND m.tokensUsed IS NOT NULL")
    Long sumTokensAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Sum cost after a certain date
     */
    @Query("SELECT COALESCE(SUM(m.costUsd), 0.0) FROM ChatMessage m WHERE m.createdAt >= :startDate AND m.costUsd IS NOT NULL")
    BigDecimal sumCostAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Average completion time after a certain date
     */
    @Query("SELECT CAST(AVG(m.completionMs) AS int) FROM ChatMessage m WHERE m.createdAt >= :startDate AND m.completionMs IS NOT NULL AND m.role = 'assistant'")
    Integer avgCompletionMsAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find popular questions (most frequently asked)
     */
    @Query(value = "SELECT m.content, COUNT(*) as count, AVG(m.completion_ms) as avg_ms " +
                   "FROM chat_messages m " +
                   "WHERE m.role = 'user' AND m.created_at >= :startDate " +
                   "GROUP BY m.content " +
                   "ORDER BY count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findPopularQuestionsAfter(@Param("startDate") LocalDateTime startDate, @Param("limit") int limit);
}
