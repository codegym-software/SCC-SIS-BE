package com.example.sis.controllers;

import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.StudentClassService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Controller để lấy danh sách lớp học của học viên
 * Base path: /api/students
 */
@RestController
@RequestMapping("/api/students")
public class StudentClassController {

    private final StudentClassService studentClassService;
    private final UserRoleRepository userRoleRepository;
    private final StudentRepository studentRepository;

    public StudentClassController(StudentClassService studentClassService, 
                                   UserRoleRepository userRoleRepository,
                                   StudentRepository studentRepository) {
        this.studentClassService = studentClassService;
        this.userRoleRepository = userRoleRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Lấy danh sách lớp học của học viên hiện tại (user đang login)
     * GET /api/students/my-classes
     */
    @GetMapping("/my-classes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassResponse>> getMyClasses(Authentication authentication) {
        // Lấy User ID từ JWT token
        Integer userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        // Tìm Student ID từ User ID
        Integer studentId = studentRepository.findStudentIdByUserId(userId);
        
        // Nếu user chưa có student profile → trả về danh sách rỗng
        if (studentId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Lấy danh sách lớp học
        List<ClassResponse> classes = studentClassService.getClassesByStudentId(studentId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Lấy danh sách lớp học của một học viên cụ thể
     * GET /api/students/{studentId}/classes
     */
    @GetMapping("/{studentId}/classes")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<List<ClassResponse>> getStudentClasses(@PathVariable Integer studentId) {
        List<ClassResponse> classes = studentClassService.getClassesByStudentId(studentId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Helper method: Lấy User ID từ JWT token
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }
}

