package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    // Performance tracking
    @Column(name = "completion_ms")
    private Integer completionMs;
    
    @Column(length = 50)
    private String model;
    
    // Cost tracking
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd;
    
    // Sources from RAG
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<MessageSource> sources;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum MessageRole {
        user, assistant, system
    }
}
