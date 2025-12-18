package com.example.sis.repository;

import com.example.sis.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Integer userId);
    
    List<ChatSession> findByUserId(Integer userId);
}
