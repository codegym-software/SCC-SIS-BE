package com.example.sis.controller;

import com.example.sis.service.system.SystemStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal API for fetching real-time system statistics
 * Used by ChatService to answer questions about system data
 */
@RestController
@RequestMapping("/api/internal/system-stats")
@RequiredArgsConstructor
@Slf4j
public class SystemStatsController {
    
    private final SystemStatsService systemStatsService;
    
    /**
     * Get total users count
     * Example question: "Hệ thống có bao nhiêu user?"
     */
    @GetMapping("/users/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTotalUsers() {
        Long count = systemStatsService.getTotalUsers();
        return ResponseEntity.ok(Map.of(
            "total_users", count,
            "description", "Tổng số người dùng trong hệ thống"
        ));
    }
    
    /**
     * Get total students count
     * Example question: "Hệ thống đang có bao nhiêu học viên?"
     */
    @GetMapping("/students/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTotalStudents() {
        Long total = systemStatsService.getTotalStudents();
        Long active = systemStatsService.getActiveStudents();
        
        return ResponseEntity.ok(Map.of(
            "total_students", total,
            "active_students", active,
            "inactive_students", total - active,
            "description", "Tổng số học viên trong hệ thống"
        ));
    }
    
    /**
     * Get class start date and countdown
     * Example question: "Lớp Java K-17 bao giờ bắt đầu?"
     */
    @GetMapping("/classes/{className}/start-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getClassStartInfo(@PathVariable String className) {
        Map<String, Object> info = systemStatsService.getClassStartInfo(className);
        return ResponseEntity.ok(info);
    }
    
    /**
     * Get comprehensive system statistics
     * Example question: "Thống kê tổng quan hệ thống?"
     */
    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        Map<String, Object> stats = systemStatsService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get enrollment stats for a class
     * Example question: "Lớp Java K-17 còn bao nhiêu chỗ trống?"
     */
    @GetMapping("/classes/{className}/enrollment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getClassEnrollmentStats(@PathVariable String className) {
        Map<String, Object> stats = systemStatsService.getClassEnrollmentStats(className);
        return ResponseEntity.ok(stats);
    }
}
