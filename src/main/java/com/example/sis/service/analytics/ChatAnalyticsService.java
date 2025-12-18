package com.example.sis.service.analytics;

import com.example.sis.dto.analytics.ChatAnalyticsDTO;
import com.example.sis.dto.analytics.PopularQuestionDTO;
import com.example.sis.repository.ChatMessageRepository;
import com.example.sis.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat Analytics Service - For Admin Dashboard
 * Provides insights into chat usage, costs, and performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAnalyticsService {
    
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    
    /**
     * Get overall chat analytics
     */
    public ChatAnalyticsDTO getOverallAnalytics(LocalDateTime startDate) {
        log.info("📊 Generating analytics from {}", startDate);
        
        // Count all sessions (not just after startDate, for total overview)
        Long totalSessions = sessionRepository.count();
        
        // Messages after start date
        Long totalMessages = messageRepository.countMessagesAfter(startDate);
        
        // Estimate user vs assistant messages (roughly 50/50)
        Long totalUserMessages = totalMessages / 2;
        Long totalAssistantMessages = totalMessages - totalUserMessages;
        
        // Token usage and costs
        Long totalTokens = messageRepository.sumTokensAfter(startDate);
        BigDecimal totalCost = messageRepository.sumCostAfter(startDate);
        
        // Average completion time (only for assistant messages)
        Integer avgCompletionMs = messageRepository.avgCompletionMsAfter(startDate);
        
        // Calculate average cost per message
        BigDecimal avgCostPerMessage = totalMessages > 0 ? 
            totalCost.divide(BigDecimal.valueOf(totalMessages), 6, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        
        log.info("✅ Analytics: {} sessions, {} messages, ${} total cost", 
            totalSessions, totalMessages, totalCost);
        
        return ChatAnalyticsDTO.builder()
            .totalSessions(totalSessions)
            .totalMessages(totalMessages)
            .totalUserMessages(totalUserMessages)
            .totalAssistantMessages(totalAssistantMessages)
            .avgCompletionMs(avgCompletionMs != null ? avgCompletionMs : 0)
            .totalTokensUsed(totalTokens)
            .totalCostUsd(totalCost)
            .avgCostPerMessage(avgCostPerMessage)
            .build();
    }
    
    /**
     * Get popular questions (most frequently asked)
     */
    public List<PopularQuestionDTO> getPopularQuestions(LocalDateTime startDate, int limit) {
        log.info("🔍 Finding top {} popular questions from {}", limit, startDate);
        
        List<Object[]> results = messageRepository.findPopularQuestionsAfter(startDate, limit);
        
        List<PopularQuestionDTO> questions = results.stream()
            .map(row -> PopularQuestionDTO.builder()
                .question((String) row[0])
                .count(((Number) row[1]).longValue())
                .avgCompletionMs(row[2] != null ? ((Number) row[2]).intValue() : 0)
                .build())
            .collect(Collectors.toList());
        
        log.info("✅ Found {} popular questions", questions.size());
        
        return questions;
    }
}
