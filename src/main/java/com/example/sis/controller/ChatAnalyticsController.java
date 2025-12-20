package com.example.sis.controller;

import com.example.sis.dto.analytics.*;
import com.example.sis.service.analytics.ChatAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat Analytics Controller - Admin Only
 * Provides insights and statistics about chat usage
 */
@RestController
@RequestMapping("/api/admin/chat-analytics")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ADMIN')") // ✅ Admin only
public class ChatAnalyticsController {
    
    private final ChatAnalyticsService analyticsService;
    
    /**
     * Get overall chat analytics
     * GET /api/admin/chat-analytics/overview?days=30
     * 
     * Example response:
     * {
     *   "totalQuestions": 150,
     *   "totalUsers": 45,
     *   "avgResponseTime": 1250,
     *   "questionsTrend": 12.5,
     *   "usersTrend": -5.2,
     *   "responseTimeTrend": 3.1
     * }
     */
    @GetMapping("/overview")
    public ResponseEntity<OverviewStatsDTO> getOverallAnalytics(
        @RequestParam(required = false, defaultValue = "30") int days
    ) {
        log.info("📊 Admin requests analytics for last {} days", days);
        
        OverviewStatsDTO overview = analyticsService.getOverviewStats(days);
        
        return ResponseEntity.ok(overview);
    }
    
    /**
     * Get popular questions (most frequently asked)
     * GET /api/admin/chat-analytics/popular-questions?days=30&limit=10
     * 
     * Example response:
     * [
     *   {
     *     "question": "Java là gì?",
     *     "count": 25,
     *     "avgCompletionMs": 1200
     *   },
     *   {
     *     "question": "Spring Boot hoạt động như thế nào?",
     *     "count": 18,
     *     "avgCompletionMs": 1500
     *   }
     * ]
     */
    @GetMapping("/popular-questions")
    public ResponseEntity<List<PopularQuestionDTO>> getPopularQuestions(
        @RequestParam(required = false, defaultValue = "30") int days,
        @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        log.info("🔍 Admin requests top {} popular questions for last {} days", limit, days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<PopularQuestionDTO> questions = analyticsService.getPopularQuestions(startDate, limit);
        
        return ResponseEntity.ok(questions);
    }
    
    /**
     * Get unanswered questions (low similarity)
     * GET /api/admin/chat-analytics/unanswered-questions?limit=5
     */
    @GetMapping("/unanswered-questions")
    public ResponseEntity<List<UnansweredQuestionDTO>> getUnansweredQuestions(
        @RequestParam(required = false, defaultValue = "5") int limit
    ) {
        log.info("❓ Admin requests unanswered questions (limit: {})", limit);
        
        List<UnansweredQuestionDTO> questions = analyticsService.getUnansweredQuestions(limit);
        
        return ResponseEntity.ok(questions);
    }
    
    /**
     * Get usage trends over time
     * GET /api/admin/chat-analytics/usage-trends?days=7
     */
    @GetMapping("/usage-trends")
    public ResponseEntity<List<UsageTrendDTO>> getUsageTrends(
        @RequestParam(required = false, defaultValue = "7") int days
    ) {
        log.info("📈 Admin requests usage trends for last {} days", days);
        
        List<UsageTrendDTO> trends = analyticsService.getUsageTrends(days);
        
        return ResponseEntity.ok(trends);
    }
    
    /**
     * Get response time distribution
     * GET /api/admin/chat-analytics/response-time-distribution
     */
    @GetMapping("/response-time-distribution")
    public ResponseEntity<List<ResponseTimeRangeDTO>> getResponseTimeDistribution() {
        log.info("⏱️ Admin requests response time distribution");
        
        List<ResponseTimeRangeDTO> distribution = analyticsService.getResponseTimeDistribution();
        
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * Get user satisfaction ratings
     * GET /api/admin/chat-analytics/user-satisfaction
     */
    @GetMapping("/user-satisfaction")
    public ResponseEntity<UserSatisfactionDTO> getUserSatisfaction() {
        log.info("⭐ Admin requests user satisfaction");
        
        UserSatisfactionDTO satisfaction = analyticsService.getUserSatisfaction();
        
        return ResponseEntity.ok(satisfaction);
    }
}
