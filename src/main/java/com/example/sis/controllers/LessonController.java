package com.example.sis.controllers;

import com.example.sis.dtos.lesson.*;
import com.example.sis.models.Lesson;
import com.example.sis.models.LessonProgress;
import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.LessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    
    @Autowired
    private LessonService lessonService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * API 1: Get lessons by module with progress
     * GET /api/lessons/module/{moduleId}
     * Quyền: Tất cả authenticated users
     * - STUDENT: Xem lessons + progress của mình
     * - ADMIN/MANAGER: Xem lessons (không có progress)
     */
    @GetMapping("/module/{moduleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonResponseDTO>> getLessonsByModule(
            @PathVariable Integer moduleId,
            Authentication authentication) {
        
        // Get student ID only if user is a student
        Integer studentId = null;
        try {
            // Chỉ lấy studentId nếu user có role STUDENT
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"))) {
                studentId = getUserIdFromAuthentication(authentication);
            }
        } catch (Exception e) {
            // Admin/Manager không có studentId, để null
        }
        
        List<LessonResponseDTO> lessons = lessonService.getLessonsByModule(moduleId, studentId);
        return ResponseEntity.ok(lessons);
    }
    
    /**
     * API NEW: Get all lessons by class (includes lessons from all semesters)
     * GET /api/lessons/class/{classId}
     * Quyền: Tất cả authenticated users
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonResponseDTO>> getLessonsByClass(
            @PathVariable Integer classId,
            Authentication authentication) {
        
        // Get student ID only if user is a student
        Integer studentId = null;
        try {
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"))) {
                studentId = getUserIdFromAuthentication(authentication);
            }
        } catch (Exception e) {
            // Admin/Manager không có studentId, để null
        }
        
        List<LessonResponseDTO> lessons = lessonService.getLessonsByClass(classId, studentId);
        return ResponseEntity.ok(lessons);
    }
    
    /**
     * API 1.5: Get single lesson by ID with progress
     * GET /api/lessons/{lessonId}
     * Quyền: Tất cả authenticated users
     */
    @GetMapping("/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonResponseDTO> getLessonById(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        
        Integer studentId = null;
        try {
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"))) {
                studentId = getUserIdFromAuthentication(authentication);
            }
        } catch (Exception e) {
            // Admin/Manager không có studentId
        }
        
        LessonResponseDTO lesson = lessonService.getLessonById(lessonId, studentId);
        return ResponseEntity.ok(lesson);
    }
    
    /**
     * API 2: Update video progress
     * POST /api/lessons/{lessonId}/progress
     * Quyền: STUDENT only (học viên tự cập nhật tiến trình học tập)
     */
    @PostMapping("/{lessonId}/progress")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<LessonProgress> updateProgress(
            @PathVariable Integer lessonId,
            @RequestBody VideoProgressRequestDTO request,
            Authentication authentication) {
        
        Integer studentId = getUserIdFromAuthentication(authentication);
        
        LessonProgress progress = lessonService.updateProgress(lessonId, studentId, request);
        return ResponseEntity.ok(progress);
    }
    
    /**
     * API 3: Get module progress
     * GET /api/lessons/module/{moduleId}/progress
     * Quyền: STUDENT, LECTURER, ACADEMIC_STAFF, CENTER_MANAGER, SUPER_ADMIN
     */
    @GetMapping("/module/{moduleId}/progress")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'LECTURER', 'ACADEMIC_STAFF', 'CENTER_MANAGER') or @authz.isSuperAdmin(authentication)")
    public ResponseEntity<ModuleProgressResponseDTO> getModuleProgress(
            @PathVariable Integer moduleId,
            Authentication authentication) {
        
        Integer studentId = getUserIdFromAuthentication(authentication);
        
        ModuleProgressResponseDTO progress = lessonService.getModuleProgress(moduleId, studentId);
        return ResponseEntity.ok(progress);
    }
    
    /**
     * API 4: Create lesson (Admin only)
     * POST /api/lessons
     * Quyền: SUPER_ADMIN, TRAINING_MANAGER
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<?> createLesson(@RequestBody CreateLessonRequestDTO request) {
        try {
            System.out.println("=== CREATE LESSON REQUEST ===");
            System.out.println("Module ID: " + request.getModuleId());
            System.out.println("Lesson Title: " + request.getLessonTitle());
            System.out.println("Lesson Type: " + request.getLessonType());
            System.out.println("Content Type: " + request.getContentType());
            
            Lesson lesson = lessonService.createLesson(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(lesson);
        } catch (Exception e) {
            System.err.println("=== ERROR CREATING LESSON ===");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to extract user ID from authentication
     */
    private Integer getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("Invalid authentication");
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakUserId = jwt.getSubject();
        
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return user.getUserId();
    }
}
