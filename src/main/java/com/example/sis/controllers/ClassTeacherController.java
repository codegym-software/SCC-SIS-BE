package com.example.sis.controllers;

import com.example.sis.dtos.classteacher.AssignLecturerRequest;
import com.example.sis.dtos.classteacher.BatchAssignLecturerRequest;
import com.example.sis.dtos.classteacher.BatchAssignLecturerResponse;
import com.example.sis.dtos.classteacher.ClassLecturerResponse;
import com.example.sis.dtos.classteacher.ClassLecturerItem;
import com.example.sis.dtos.classteacher.LecturerLite;
import com.example.sis.dtos.classteacher.ListResponse;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ClassTeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassTeacherController {

    private final ClassTeacherService classTeacherService;
    private final UserRoleRepository userRoleRepository;

    public ClassTeacherController(ClassTeacherService classTeacherService,
            UserRoleRepository userRoleRepository) {
        this.classTeacherService = classTeacherService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Gán lecturer vào lớp học
     * Chỉ ACADEMIC_STAFF hoặc SUPER_ADMIN mới có quyền
     */
    @PostMapping("/{classId}/lecturers/{lecturerId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ClassLecturerResponse> assignLecturer(
            @PathVariable Integer classId,
            @PathVariable Integer lecturerId,
            @Valid @RequestBody AssignLecturerRequest request,
            Authentication authentication) {

        Integer assignedBy = getCurrentUserId(authentication);
        ClassLecturerResponse response = classTeacherService.assignLecturer(classId, lecturerId, request, assignedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Revoke assignment (soft delete) - implementation theo yêu cầu
     */
    @DeleteMapping("/{classId}/lecturers/{assignmentId}")
    @PreAuthorize("@authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<Void> revoke(
            @PathVariable Integer classId,
            @PathVariable Long assignmentId) {

        classTeacherService.revokeAssignment(classId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy danh sách lecturers hiện tại của lớp (active assignments)
     * Tất cả authenticated users có thể xem
     */
    @GetMapping("/{classId}/lecturers")
    public ResponseEntity<ListResponse<ClassLecturerItem>> getActiveLecturers(
            @PathVariable Integer classId,
            @RequestParam(required = false) String q) {

        ListResponse<ClassLecturerItem> response = classTeacherService.getActiveLecturers(classId, q);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tất cả lecturer assignments của lớp (bao gồm cả inactive)
     * Chỉ ACADEMIC_STAFF hoặc SUPER_ADMIN mới có quyền xem lịch sử
     */
    @GetMapping("/{classId}/lecturers/all")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ListResponse<ClassLecturerItem>> getAllLecturers(
            @PathVariable Integer classId,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) String q) {

        ListResponse<ClassLecturerItem> response = classTeacherService.getAllLecturers(classId, status, q);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách giảng viên available để phân công cho lớp
     * Chỉ ACADEMIC_STAFF hoặc SUPER_ADMIN mới có quyền
     */
    @GetMapping("/{classId}/lecturers/available")
    @PreAuthorize("@authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<ListResponse<LecturerLite>> getAvailableLecturers(
            @PathVariable Integer classId,
            @RequestParam(required = false) String q) {

        ListResponse<LecturerLite> response = classTeacherService.getAvailableLecturers(classId, q);
        return ResponseEntity.ok(response);
    }

    /**
     * Gán nhiều giảng viên cho lớp (batch assignment)
     * Chỉ ACADEMIC_STAFF hoặc SUPER_ADMIN mới có quyền
     */
    @PostMapping("/{classId}/lecturers/batch")
    @PreAuthorize("@authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<BatchAssignLecturerResponse> batchAssignLecturers(
            @PathVariable Integer classId,
            @Valid @RequestBody BatchAssignLecturerRequest request,
            Authentication authentication) {

        Integer assignedBy = getCurrentUserId(authentication);
        BatchAssignLecturerResponse response = classTeacherService.batchAssignLecturers(classId, request, assignedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy User ID hiện tại từ JWT token
     * Sử dụng pattern giống như ClassController
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject(); // Lấy sub claim từ JWT
            System.out.println("Debug - Keycloak User ID from JWT: " + keycloakUserId);

            if (keycloakUserId == null || keycloakUserId.isBlank()) {
                System.err.println("ERROR - Keycloak User ID is null or blank");
                return null;
            }

            // Tìm user ID trong database dựa trên keycloak_user_id
            Integer userId = userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
            System.out.println("Debug - Found User ID in DB: " + userId);

            if (userId == null) {
                System.err.println("ERROR - User not found in DB for keycloak ID: " + keycloakUserId);
            }

            return userId;
        }
        System.err.println("ERROR - Authentication or JWT is null");
        return null;
    }
}