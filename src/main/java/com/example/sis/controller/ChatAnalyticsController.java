package com.example.sis.controller;

import com.example.sis.dto.analytics.ChatAnalyticsDTO;
import com.example.sis.dto.analytics.PopularQuestionDTO;
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
     *   "totalSessions": 150,
     *   "totalMessages": 3000,
     *   "totalUserMessages": 1500,
     *   "totalAssistantMessages": 1500,
     *   "avgCompletionMs": 1250,
     *   "totalTokensUsed": 45000,
     *   "totalCostUsd": 0.15,
     *   "avgCostPerMessage": 0.00005
     * }
     */
    @GetMapping("/overview")
    public ResponseEntity<ChatAnalyticsDTO> getOverallAnalytics(
        @RequestParam(required = false, defaultValue = "30") int days
    ) {
        log.info("📊 Admin requests analytics for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        ChatAnalyticsDTO analytics = analyticsService.getOverallAnalytics(startDate);
        
        return ResponseEntity.ok(analytics);
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
}
