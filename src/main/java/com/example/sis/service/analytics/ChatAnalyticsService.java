package com.example.sis.service.analytics;

import com.example.sis.dto.analytics.*;
import com.example.sis.repository.ChatMessageRepository;
import com.example.sis.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
     * Get overview statistics with trends
     */
    public OverviewStatsDTO getOverviewStats(int days) {
        log.info("📊 Generating overview stats for last {} days", days);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentPeriodStart = now.minusDays(days);
        LocalDateTime previousPeriodStart = now.minusDays(days * 2);
        
        // Current period stats
        Long currentQuestions = messageRepository.countMessagesBetween(currentPeriodStart, now) / 2;
        Long currentUsers = sessionRepository.countSessionsBetween(currentPeriodStart, now);
        Integer currentAvgResponseTime = messageRepository.avgCompletionMsBetween(currentPeriodStart, now);
        
        // Previous period stats for trend calculation
        Long previousQuestions = messageRepository.countMessagesBetween(previousPeriodStart, currentPeriodStart) / 2;
        Long previousUsers = sessionRepository.countSessionsBetween(previousPeriodStart, currentPeriodStart);
        Integer previousAvgResponseTime = messageRepository.avgCompletionMsBetween(previousPeriodStart, currentPeriodStart);
        
        // Calculate trends (percentage change)
        Double questionsTrend = calculateTrend(previousQuestions, currentQuestions);
        Double usersTrend = calculateTrend(previousUsers, currentUsers);
        Double responseTimeTrend = calculateTrend(
            previousAvgResponseTime != null ? previousAvgResponseTime.longValue() : 0L,
            currentAvgResponseTime != null ? currentAvgResponseTime.longValue() : 0L
        );
        
        log.info("✅ Overview: {} questions, {} users, {}ms avg response time", 
            currentQuestions, currentUsers, currentAvgResponseTime);
        
        return OverviewStatsDTO.builder()
            .totalQuestions(currentQuestions)
            .totalUsers(currentUsers)
            .avgResponseTime(currentAvgResponseTime != null ? currentAvgResponseTime : 0)
            .questionsTrend(questionsTrend)
            .usersTrend(usersTrend)
            .responseTimeTrend(responseTimeTrend)
            .build();
    }
    
    private Double calculateTrend(Long previous, Long current) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double change = ((current - previous) * 100.0) / previous;
        return Math.round(change * 10.0) / 10.0;
    }
    
    /**
     * Get overall chat analytics (legacy)
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
                .satisfactionRate(85.0) // Default value - would need rating data per question
                .build())
            .collect(Collectors.toList());
        
        log.info("✅ Found {} popular questions", questions.size());
        
        return questions;
    }
    
    /**
     * Get unanswered questions (questions with response time 30-45s)
     */
    public List<UnansweredQuestionDTO> getUnansweredQuestions(int limit) {
        log.info("❓ Finding questions needing improvement (response time 30-45s, limit: {})", limit);
        
        // Find questions with slow response time (30-45 seconds)
        List<Object[]> results = messageRepository.findSlowResponseQuestionsInRange(30000, 45000, limit);
        
        List<UnansweredQuestionDTO> questions = results.stream()
            .map(row -> {
                Long messageId = ((Number) row[0]).longValue();
                String content = (String) row[1];
                Integer completionMs = row[2] != null ? ((Number) row[2]).intValue() : 0;
                Integer askedCount = row[3] != null ? ((Number) row[3]).intValue() : 0;
                LocalDateTime lastAsked = row[4] != null ? ((java.sql.Timestamp) row[4]).toLocalDateTime() : LocalDateTime.now();
                
                // Calculate similarity score (inverse of response time - slower = lower similarity)
                // 30s = 0.5, 45s = 0.33
                double avgSimilarity = Math.max(0.3, Math.min(0.5, 15000.0 / completionMs));
                
                return UnansweredQuestionDTO.builder()
                    .questionId(messageId)
                    .question(content)
                    .avgSimilarity(Math.round(avgSimilarity * 100.0) / 100.0)
                    .askedCount(askedCount)
                    .lastAsked(lastAsked)
                    .build();
            })
            .collect(Collectors.toList());
        
        log.info("✅ Found {} questions needing improvement", questions.size());
        
        return questions;
    }
    
    /**
     * Get usage trends over time
     */
    public List<UsageTrendDTO> getUsageTrends(int days) {
        log.info("📈 Calculating usage trends for last {} days", days);
        
        List<UsageTrendDTO> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            Long messageCount = messageRepository.countMessagesBetween(startOfDay, endOfDay);
            Long sessionCount = sessionRepository.countSessionsBetween(startOfDay, endOfDay);
            Integer avgResponseTime = messageRepository.avgCompletionMsBetween(startOfDay, endOfDay);
            
            trends.add(UsageTrendDTO.builder()
                .date(date.format(formatter))
                .questionCount(messageCount / 2) // Estimate user messages
                .userCount(sessionCount) // Use session count as proxy for users
                .avgResponseTime(avgResponseTime != null ? avgResponseTime : 0)
                .build());
        }
        
        log.info("✅ Generated {} trend data points", trends.size());
        return trends;
    }
    
    /**
     * Get response time distribution
     */
    public List<ResponseTimeRangeDTO> getResponseTimeDistribution() {
        log.info("⏱️ Calculating response time distribution");
        
        Long totalMessages = messageRepository.count();
        if (totalMessages == 0) {
            return Collections.emptyList();
        }
        
        // Count messages in different time ranges (more detailed)
        Long under1s = messageRepository.countByCompletionMsLessThan(1000);
        Long between1and3s = messageRepository.countByCompletionMsBetween(1000, 3000);
        Long between3and5s = messageRepository.countByCompletionMsBetween(3000, 5000);
        Long between5and10s = messageRepository.countByCompletionMsBetween(5000, 10000);
        Long between10and15s = messageRepository.countByCompletionMsBetween(10000, 15000);
        Long between15and30s = messageRepository.countByCompletionMsBetween(15000, 30000);
        Long between30and45s = messageRepository.countByCompletionMsBetween(30000, 45000);
        Long between45and60s = messageRepository.countByCompletionMsBetween(45000, 60000);
        Long over60s = messageRepository.countByCompletionMsGreaterThan(60000);
        
        List<ResponseTimeRangeDTO> distribution = new ArrayList<>();
        distribution.add(createRangeDTO("< 1s", under1s, totalMessages));
        distribution.add(createRangeDTO("1-3s", between1and3s, totalMessages));
        distribution.add(createRangeDTO("3-5s", between3and5s, totalMessages));
        distribution.add(createRangeDTO("5-10s", between5and10s, totalMessages));
        distribution.add(createRangeDTO("10-15s", between10and15s, totalMessages));
        distribution.add(createRangeDTO("15-30s", between15and30s, totalMessages));
        distribution.add(createRangeDTO("30-45s", between30and45s, totalMessages));
        distribution.add(createRangeDTO("45-60s", between45and60s, totalMessages));
        distribution.add(createRangeDTO("> 60s", over60s, totalMessages));
        
        log.info("✅ Generated response time distribution with 9 ranges");
        return distribution;
    }
    
    /**
     * Get user satisfaction ratings (mock data)
     */
    public UserSatisfactionDTO getUserSatisfaction() {
        log.info("⭐ Calculating user satisfaction (using mock data)");
        
        // Mock data - would need actual rating system
        List<UserSatisfactionDTO.RatingDistributionDTO> distribution = new ArrayList<>();
        distribution.add(UserSatisfactionDTO.RatingDistributionDTO.builder().rating(1).count(2L).build());
        distribution.add(UserSatisfactionDTO.RatingDistributionDTO.builder().rating(2).count(5L).build());
        distribution.add(UserSatisfactionDTO.RatingDistributionDTO.builder().rating(3).count(15L).build());
        distribution.add(UserSatisfactionDTO.RatingDistributionDTO.builder().rating(4).count(45L).build());
        distribution.add(UserSatisfactionDTO.RatingDistributionDTO.builder().rating(5).count(33L).build());
        
        double avgRating = 4.0;
        long feedbackCount = 100L;
        
        log.info("✅ Mock avg rating: {}, {} total ratings", avgRating, feedbackCount);
        
        return UserSatisfactionDTO.builder()
            .avgRating(avgRating)
            .ratingDistribution(distribution)
            .feedbackCount(feedbackCount)
            .build();
    }
    
    private ResponseTimeRangeDTO createRangeDTO(String range, Long count, Long total) {
        double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
        return ResponseTimeRangeDTO.builder()
            .range(range)
            .count(count)
            .percentage(Math.round(percentage * 10.0) / 10.0)
            .build();
    }
}
