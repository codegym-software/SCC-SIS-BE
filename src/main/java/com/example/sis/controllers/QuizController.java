package com.example.sis.controllers;

import com.example.sis.dtos.quiz.*;
import com.example.sis.models.Quiz;
import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.QuizAttemptService;
import com.example.sis.services.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    
    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
    
    @Autowired
    private QuizService quizService;
    
    @Autowired
    private QuizAttemptService attemptService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * API 1: Tạo Quiz mới (Admin only)
     * POST /api/quizzes
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<?> createQuiz(@RequestBody CreateQuizDTO request) {
        try {
            logger.info("=== CREATE QUIZ REQUEST ===");
            logger.info("Lesson ID: {}", request.getLessonId());
            logger.info("Quiz Title: {}", request.getQuizTitle());
            
            Quiz quiz = quizService.createQuiz(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(quiz);
        } catch (Exception e) {
            logger.error("Error creating quiz: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 2: Import câu hỏi từ file Word (Admin only)
     * POST /api/quizzes/{quizId}/questions/import
     */
    @PostMapping("/{quizId}/questions/import")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<?> importQuestions(
            @PathVariable Integer quizId,
            @RequestParam("file") MultipartFile file) {
        try {
            logger.info("=== IMPORT QUESTIONS FROM WORD ===");
            logger.info("Quiz ID: {}", quizId);
            logger.info("File: {}", file.getOriginalFilename());
            
            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".docx")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only .docx files are supported");
            }
            
            String result = quizService.importQuestionsFromWord(quizId, file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Error reading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error importing questions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 3: Lấy chi tiết Quiz
     * GET /api/quizzes/lesson/{lessonId}
     * - Admin: Xem có đáp án (includeAnswers=true)
     * - Student: Xem không có đáp án (includeAnswers=false)
     */
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getQuizByLesson(
            @PathVariable Integer lessonId,
            @RequestParam(defaultValue = "false") boolean includeAnswers,
            Authentication authentication) {
        try {
            // Check if user is admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN") || 
                                     auth.getAuthority().equals("ROLE_TRAINING_MANAGER"));
            
            // Chỉ admin mới được xem đáp án
            boolean showAnswers = includeAnswers && isAdmin;
            
            QuizDetailDTO quiz = quizService.getQuizDetail(lessonId, showAnswers);
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            logger.error("Error getting quiz: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 4: Bắt đầu làm Quiz (Student only)
     * POST /api/quiz-attempts/start
     */
    @PostMapping("/attempts/start")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<?> startAttempt(
            @RequestBody StartAttemptRequestDTO request,
            Authentication authentication) {
        try {
            Integer studentId = getUserIdFromAuthentication(authentication);
            
            logger.info("=== START QUIZ ATTEMPT ===");
            logger.info("Student ID: {}", studentId);
            logger.info("Quiz ID: {}", request.getQuizId());
            
            StartAttemptResponseDTO response = attemptService.startAttempt(studentId, request.getQuizId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error starting attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 5: Lưu nhiều câu trả lời cùng lúc (Student only)
     * POST /api/quizzes/attempts/{attemptId}/answers
     */
    @PostMapping("/attempts/{attemptId}/answers")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<?> submitAnswers(
            @PathVariable Integer attemptId,
            @RequestBody BulkAnswersRequestDTO request,
            Authentication authentication) {
        try {
            Integer studentId = getUserIdFromAuthentication(authentication);
            logger.info("=== STUDENT SUBMITTING ANSWERS ===");
            logger.info("Student ID: {}, Attempt ID: {}, Total Answers: {}", 
                    studentId, attemptId, request.getAnswers().size());
            
            attemptService.saveBulkAnswers(studentId, attemptId, request.getAnswers());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Saved " + request.getAnswers().size() + " answers successfully");
            response.put("totalAnswers", request.getAnswers().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving answers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 6: Submit Quiz và lấy kết quả (Student only)
     * POST /api/quiz-attempts/{attemptId}/submit
     */
    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<?> submitQuiz(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        try {
            Integer studentId = getUserIdFromAuthentication(authentication);
            
            logger.info("=== SUBMIT QUIZ ===");
            logger.info("Attempt ID: {}", attemptId);
            logger.info("Student ID: {}", studentId);
            
            QuizResultDTO result = attemptService.submitQuiz(studentId, attemptId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error submitting quiz: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 7: Xem lịch sử làm bài (Student only)
     * GET /api/quiz-attempts/quiz/{quizId}/my-attempts
     */
    @GetMapping("/attempts/quiz/{quizId}/my-attempts")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<?> getMyAttempts(
            @PathVariable Integer quizId,
            Authentication authentication) {
        try {
            Integer studentId = getUserIdFromAuthentication(authentication);
            
            AttemptHistoryDTO history = attemptService.getAttemptHistory(studentId, quizId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error getting attempt history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * API 8: Xem chi tiết kết quả của một attempt cụ thể (Student only)
     * GET /api/quizzes/attempts/{attemptId}/result
     */
    @GetMapping("/attempts/{attemptId}/result")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<?> getAttemptResult(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        try {
            Integer studentId = getUserIdFromAuthentication(authentication);
            QuizResultDTO result = attemptService.getAttemptResult(attemptId, studentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting attempt result: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to get user ID from authentication
     */
    private Integer getUserIdFromAuthentication(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String email = jwt.getClaimAsString("email");
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        return user.getUserId();
    }
}
