package com.example.sis.controllers;

import com.example.sis.dtos.classteacher.AssignLecturerRequest;
import com.example.sis.dtos.classteacher.ClassLecturerResponse;
import com.example.sis.dtos.classteacher.RemoveLecturerRequest;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ClassTeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PostMapping("/{classId}/lecturers")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ClassLecturerResponse> assignLecturer(
            @PathVariable Integer classId,
            @Valid @RequestBody AssignLecturerRequest request,
            Authentication authentication) {

        Integer assignedBy = getCurrentUserId(authentication);
        ClassLecturerResponse response = classTeacherService.assignLecturer(classId, request, assignedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Xóa lecturer khỏi lớp học
     * Chỉ ACADEMIC_STAFF hoặc SUPER_ADMIN mới có quyền
     */
    @DeleteMapping("/{classId}/lecturers")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<Void> removeLecturer(
            @PathVariable Integer classId,
            @Valid @RequestBody RemoveLecturerRequest request,
            Authentication authentication) {

        Integer revokedBy = getCurrentUserId(authentication);
        classTeacherService.removeLecturer(classId, request, revokedBy);

        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy danh sách lecturers hiện tại của lớp (active assignments)
     * Tất cả authenticated users có thể xem
     */
    @GetMapping("/{classId}/lecturers")
    public ResponseEntity<List<ClassLecturerResponse>> getActiveLecturers(
            @PathVariable Integer classId) {

        List<ClassLecturerResponse> lecturers = classTeacherService.getActiveLecturers(classId);
        return ResponseEntity.ok(lecturers);
    }

    /**
     * Lấy tất cả lecturer assignments của lớp (bao gồm cả inactive)
     * Chỉ ACADEMIC_STAFF hoặc SUPER_ADMIN mới có quyền xem lịch sử
     */
    @GetMapping("/{classId}/lecturers/all")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<List<ClassLecturerResponse>> getAllLecturers(
            @PathVariable Integer classId) {

        List<ClassLecturerResponse> lecturers = classTeacherService.getAllLecturers(classId);
        return ResponseEntity.ok(lecturers);
    }

    /**
     * Lấy User ID hiện tại từ JWT token
     * Sử dụng pattern giống như ClassController
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject(); // Lấy sub claim từ JWT
            System.out.println("Debug - Keycloak User ID from JWT: " + keycloakUserId);

            // Tìm user ID trong database dựa trên keycloak_user_id
            Integer userId = userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
            System.out.println("Debug - Found User ID in DB: " + userId);

            return userId;
        }
        return null;
    }
}