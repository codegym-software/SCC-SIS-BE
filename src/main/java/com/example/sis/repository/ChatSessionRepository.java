package com.example.sis.repository;

import com.example.sis.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Integer userId);
    
    List<ChatSession> findByUserId(Integer userId);
    
    /**
     * Count sessions created between dates
     */
    @Query("SELECT COUNT(s) FROM ChatSession s WHERE s.createdAt >= :startDate AND s.createdAt < :endDate")
    Long countSessionsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
