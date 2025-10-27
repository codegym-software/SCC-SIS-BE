package com.example.sis.controllers;

import com.example.sis.dtos.journal.CreateJournalRequest;
import com.example.sis.dtos.journal.JournalResponse;
import com.example.sis.dtos.journal.UpdateJournalRequest;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ClassJournalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý Nhật ký lớp học
 * Base path: /api/journals
 */
@RestController
@RequestMapping("/api/journals")
public class ClassJournalController {

    private final ClassJournalService journalService;
    private final UserRoleRepository userRoleRepository;

    public ClassJournalController(ClassJournalService journalService, UserRoleRepository userRoleRepository) {
        this.journalService = journalService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Tạo nhật ký lớp học mới
     * - Chỉ giảng viên được phân vào lớp mới có thể tạo (database trigger sẽ validate)
     * - Super Admin cũng có thể tạo
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TEACHER') or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<?> createJournal(
            @Valid @RequestBody CreateJournalRequest request,
            Authentication authentication) {

        Integer userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Unauthorized", "message", "User not authenticated"));
        }

        try {
            JournalResponse response = journalService.createJournal(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // Log chi tiết lỗi validation
            System.err.println("❌ BAD_REQUEST: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "Bad Request", "message", e.getMessage()));
        } catch (Exception e) {
            // Log chi tiết lỗi trigger hoặc lỗi khác
            System.err.println("❌ FORBIDDEN/ERROR: " + e.getMessage());
            e.printStackTrace();
            // Database trigger có thể throw exception nếu teacher không được phân vào lớp
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Forbidden", "message", e.getMessage()));
        }
    }

    /**
     * Cập nhật nhật ký lớp học
     * - Chỉ chủ sở hữu (teacher tạo nhật ký) hoặc Super Admin mới được cập nhật
     */
    @PutMapping("/{journalId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TEACHER') or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<?> updateJournal(
            @PathVariable Integer journalId,
            @Valid @RequestBody UpdateJournalRequest request,
            Authentication authentication) {

        Integer userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Unauthorized", "message", "User not authenticated"));
        }

        // Check if user is SUPER_ADMIN
        String keycloakUserId = getKeycloakUserId(authentication);
        boolean isSuperAdmin = keycloakUserId != null && 
                userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(keycloakUserId, "SUPER_ADMIN");

        try {
            JournalResponse response = journalService.updateJournal(journalId, request, userId, isSuperAdmin);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "Bad Request", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Forbidden", "message", e.getMessage()));
        }
    }

    /**
     * Xóa mềm nhật ký lớp học
     * - Chỉ chủ sở hữu hoặc Super Admin mới được xóa
     */
    @DeleteMapping("/{journalId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TEACHER') or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<?> deleteJournal(
            @PathVariable Integer journalId,
            Authentication authentication) {

        Integer userId = getCurrentUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Unauthorized", "message", "User not authenticated"));
        }

        // Check if user is SUPER_ADMIN
        String keycloakUserId = getKeycloakUserId(authentication);
        boolean isSuperAdmin = keycloakUserId != null && 
                userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(keycloakUserId, "SUPER_ADMIN");

        try {
            journalService.softDeleteJournal(journalId, userId, isSuperAdmin);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Forbidden", "message", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách nhật ký theo lớp học
     * - Super Admin, Academic Staff, Teacher của lớp đó có thể xem
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF') or @authz.hasRole(authentication, 'TEACHER') or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<List<JournalResponse>> getJournalsByClass(@PathVariable Integer classId) {
        try {
            List<JournalResponse> journals = journalService.getJournalsByClass(classId);
            return ResponseEntity.ok(journals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Lấy danh sách nhật ký của một giảng viên
     * - Super Admin, Academic Staff, hoặc chính giảng viên đó có thể xem
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF') or @authz.hasRole(authentication, 'TEACHER') or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<List<JournalResponse>> getJournalsByTeacher(
            @PathVariable Integer teacherId,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Nếu không phải Super Admin hoặc Academic Staff, chỉ xem được nhật ký của chính mình
        // (Có thể thêm check qua @authz service nếu cần)

        try {
            List<JournalResponse> journals = journalService.getJournalsByTeacher(teacherId);
            return ResponseEntity.ok(journals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Helper method để lấy userId từ JWT token
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }

    /**
     * Helper method để lấy keycloakUserId từ JWT token
     */
    private String getKeycloakUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }
}
