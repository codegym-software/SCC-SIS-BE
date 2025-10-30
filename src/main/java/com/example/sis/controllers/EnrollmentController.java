package com.example.sis.controllers;

import com.example.sis.dtos.enrollment.EnrollmentRequest;
import com.example.sis.dtos.enrollment.EnrollmentResponse;
import com.example.sis.dtos.enrollment.UpdateEnrollmentRequest;
import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * /api/classes/{classId}/students
 * GET    : Xem/Lọc (SA | Academic Staff | Lecturer được phân công)
 * POST   : Thêm (SA | Academic Staff)
 * PATCH  : Cập nhật (SA | Academic Staff)
 * DELETE : Xóa mềm (SA | Academic Staff)
 */
@RestController
@RequestMapping("/api/classes/{classId}/students")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final UserRoleRepository userRoleRepo;

    public EnrollmentController(EnrollmentService enrollmentService,
                                UserRoleRepository userRoleRepo) {
        this.enrollmentService = enrollmentService;
        this.userRoleRepo = userRoleRepo;
    }

    // ===== List + Filter (cho phép Lecturer xem) =====
    @GetMapping
    @PreAuthorize("@authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<Page<EnrollmentResponse>> list(
            @PathVariable Integer classId,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(enrollmentService.list(classId, status, page, size, sort));
    }

    // ===== Enroll (idempotent) =====
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable Integer classId,
            @Valid @RequestBody EnrollmentRequest request,
            Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(enrollmentService.enroll(classId, request, userId));
    }

    // ===== Update trạng thái/leftAt =====
    @PatchMapping("/{enrollmentId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<EnrollmentResponse> update(
            @PathVariable Integer classId,
            @PathVariable Integer enrollmentId,
            @Valid @RequestBody UpdateEnrollmentRequest request,
            Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(enrollmentService.update(classId, enrollmentId, request, userId));
    }

    // ===== Soft remove (DROPPED + leftAt + revokedBy/At + note) =====
    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<Void> remove(
            @PathVariable Integer classId,
            @PathVariable Integer enrollmentId,
            @RequestParam(name = "reason", required = false) String reason,
            Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        enrollmentService.remove(classId, enrollmentId, userId, (reason == null ? "" : reason));
        return ResponseEntity.noContent().build();
    }

    // ===== Helpers =====
    private Integer getCurrentUserId(Authentication authentication) {
        String sub = getSub(authentication);
        return (sub == null) ? null : userRoleRepo.findUserIdByKeycloakUserId(sub);
    }

    private String getSub(Authentication authentication) {
        if (authentication == null) return null;
        Object p = authentication.getPrincipal();
        return (p instanceof Jwt jwt) ? jwt.getClaimAsString("sub") : null;
    }
}
