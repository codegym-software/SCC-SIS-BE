package com.example.sis.controllers;

import com.example.sis.dtos.lessons.LessonProgressUpdateRequest;
import com.example.sis.dtos.lessons.LessonProgressResponse;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.LessonProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lesson-progress")
public class LessonProgressController {
    
    private final LessonProgressService lessonProgressService;
    private final StudentRepository studentRepository;
    private final UserRoleRepository userRoleRepository;
    
    public LessonProgressController(
            LessonProgressService lessonProgressService,
            StudentRepository studentRepository,
            UserRoleRepository userRoleRepository) {
        this.lessonProgressService = lessonProgressService;
        this.studentRepository = studentRepository;
        this.userRoleRepository = userRoleRepository;
    }
    
    /**
     * Update video progress for current student
     * POST /api/lesson-progress/video
     */
    @PostMapping("/video")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<LessonProgressResponse> updateVideoProgress(
            @RequestBody LessonProgressUpdateRequest request,
            Authentication authentication) {
        
        Integer userId = getCurrentUserId(authentication);
        Integer studentId = getStudentIdFromUserId(userId);
        
        LessonProgressResponse response = lessonProgressService.updateVideoProgress(studentId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get progress for a specific lesson
     * GET /api/lesson-progress/lesson/{lessonId}
     */
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<LessonProgressResponse> getProgressForLesson(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        
        Integer userId = getCurrentUserId(authentication);
        Integer studentId = getStudentIdFromUserId(userId);
        
        LessonProgressResponse response = lessonProgressService.getProgress(studentId, lessonId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get progress for multiple lessons (bulk query for module view)
     * POST /api/lesson-progress/bulk
     */
    @PostMapping("/bulk")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<Map<Integer, LessonProgressResponse>> getProgressForLessons(
            @RequestBody List<Integer> lessonIds,
            Authentication authentication) {
        
        Integer userId = getCurrentUserId(authentication);
        Integer studentId = getStudentIdFromUserId(userId);
        
        Map<Integer, LessonProgressResponse> response = 
            lessonProgressService.getProgressForLessons(studentId, lessonIds);
        return ResponseEntity.ok(response);
    }
    
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }
    
    private Integer getStudentIdFromUserId(Integer userId) {
        return studentRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found for userId: " + userId))
            .getStudentId();
    }
}
