package com.example.sis.controllers;

import com.example.sis.dtos.attendance.*;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRoleRepository userRoleRepository;

    public AttendanceController(AttendanceService attendanceService, UserRoleRepository userRoleRepository) {
        this.attendanceService = attendanceService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * GET /api/attendance-schedules?teacher_id={teacher_id}&from={yyyy-mm-dd}&to={yyyy-mm-dd}
     * Lấy lịch dạy của giảng viên
     * - Super Admin: có thể xem
     * - Lecturer: có thể xem lịch dạy của mình
     */
    @GetMapping("/attendance-schedules")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<List<TeacherScheduleResponse>> getTeacherSchedule(
            @RequestParam Integer teacher_id,
            @RequestParam String from,
            @RequestParam String to,
            Authentication authentication) {
        
        try {
            // Nếu là Lecturer, chỉ được xem lịch của chính mình
            if (!isCurrentUserSuperAdmin(authentication)) {
                Integer currentUserId = getCurrentUserId(authentication);
                if (currentUserId == null || !currentUserId.equals(teacher_id)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);

            List<TeacherScheduleResponse> schedules = attendanceService.getTeacherSchedule(teacher_id, fromDate, toDate);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            // Log error for debugging
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * POST /api/attendance-sessions
     * Tạo buổi điểm danh mới
     * - Super Admin: có thể tạo
     * - Lecturer: có thể tạo
     */
    @PostMapping("/attendance-sessions")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<AttendanceSessionResponse> createAttendanceSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Nếu là Lecturer, chỉ được tạo điểm danh cho lớp mình dạy
        if (!isCurrentUserSuperAdmin(authentication)) {
            // TODO: Check if lecturer is assigned to this class
            // This would require checking class_teachers table
        }

        try {
            AttendanceSessionResponse response = attendanceService.createSession(request, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * GET /api/classes/{class_id}/attendance-sessions
     * Lấy danh sách buổi điểm danh của một lớp
     * - Super Admin: có thể xem
     * - Lecturer: có thể xem lớp mình dạy
     * - Academic Staff: có thể xem lớp trong trung tâm
     */
    @GetMapping("/classes/{class_id}/attendance-sessions")
    public ResponseEntity<List<AttendanceSessionSummaryResponse>> getAttendanceSessionsByClass(
            @PathVariable("class_id") Integer classId) {
        List<AttendanceSessionSummaryResponse> sessions = attendanceService.getSessionsByClass(classId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/attendance-sessions/{session_id}
     * Lấy chi tiết một buổi điểm danh
     * - Super Admin: có thể xem
     * - Lecturer: có thể xem lớp mình dạy
     * - Academic Staff: có thể xem lớp trong trung tâm
     */
    @GetMapping("/attendance-sessions/{session_id}")
    public ResponseEntity<AttendanceSessionResponse> getAttendanceSessionDetail(
            @PathVariable("session_id") Integer sessionId) {
        try {
            AttendanceSessionResponse response = attendanceService.getSessionDetail(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * PUT /api/attendance-sessions/{session_id}
     * Cập nhật buổi điểm danh
     * - Super Admin: có thể cập nhật
     * - Lecturer: có thể cập nhật lớp mình dạy
     */
    @PutMapping("/attendance-sessions/{session_id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<AttendanceSessionResponse> updateAttendanceSession(
            @PathVariable("session_id") Integer sessionId,
            @Valid @RequestBody UpdateAttendanceSessionRequest request,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: Nếu là Lecturer, check quyền sửa buổi điểm danh này

        try {
            AttendanceSessionResponse response = attendanceService.updateSession(sessionId, request, currentUserId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * DELETE /api/attendance-sessions/{session_id}
     * Xóa buổi điểm danh (soft delete)
     * - Super Admin: có thể xóa
     * - Lecturer: có thể xóa lớp mình dạy
     */
    @DeleteMapping("/attendance-sessions/{session_id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<Void> deleteAttendanceSession(
            @PathVariable("session_id") Integer sessionId,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: Nếu là Lecturer, check quyền xóa buổi điểm danh này

        try {
            attendanceService.deleteSession(sessionId, currentUserId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Helper methods
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }

    private boolean isCurrentUserSuperAdmin(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            return userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(sub, "SUPER_ADMIN");
        }
        return false;
    }
}

