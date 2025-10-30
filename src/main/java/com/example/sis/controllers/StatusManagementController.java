package com.example.sis.controllers;

import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.enums.OverallStatus;
import com.example.sis.services.StatusManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/status-management")
public class StatusManagementController {
    
    private final StatusManagementService statusManagementService;
    
    public StatusManagementController(StatusManagementService statusManagementService) {
        this.statusManagementService = statusManagementService;
    }
    
    /**
     * Chuyển đổi trạng thái Enrollment
     * PUT /api/status-management/enrollment/{enrollmentId}
     */
    @PutMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'STAFF')")
    public ResponseEntity<Map<String, String>> changeEnrollmentStatus(
            @PathVariable Integer enrollmentId,
            @RequestBody Map<String, String> request) {
        
        try {
            String statusStr = request.get("status");
            String note = request.get("note");
            
            if (statusStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Trạng thái không được để trống"));
            }
            
            EnrollmentStatus newStatus;
            try {
                newStatus = EnrollmentStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Trạng thái không hợp lệ: " + statusStr));
            }
            
            statusManagementService.changeEnrollmentStatus(enrollmentId, newStatus, null, note);
            
            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật trạng thái enrollment thành công",
                "enrollmentId", enrollmentId.toString(),
                "newStatus", newStatus.toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Lỗi khi cập nhật trạng thái: " + e.getMessage()));
        }
    }
    
    /**
     * Chuyển đổi trạng thái Student
     * PUT /api/status-management/student/{studentId}
     */
    @PutMapping("/student/{studentId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'STAFF')")
    public ResponseEntity<Map<String, String>> changeStudentStatus(
            @PathVariable Integer studentId,
            @RequestBody Map<String, String> request) {
        
        try {
            String statusStr = request.get("status");
            String note = request.get("note");
            
            if (statusStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Trạng thái không được để trống"));
            }
            
            OverallStatus newStatus;
            try {
                newStatus = OverallStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Trạng thái không hợp lệ: " + statusStr));
            }
            
            statusManagementService.changeStudentStatus(studentId, newStatus, null, note);
            
            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật trạng thái student thành công",
                "studentId", studentId.toString(),
                "newStatus", newStatus.toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Lỗi khi cập nhật trạng thái: " + e.getMessage()));
        }
    }
    
    /**
     * Tự động tốt nghiệp tất cả học viên trong lớp
     * POST /api/status-management/class/{classId}/graduate
     */
    @PostMapping("/class/{classId}/graduate")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'STAFF')")
    public ResponseEntity<Map<String, String>> graduateClassStudents(@PathVariable Integer classId) {
        
        try {
            statusManagementService.autoGraduateClassStudents(classId, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Đã tốt nghiệp tất cả học viên trong lớp",
                "classId", classId.toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Lỗi khi tốt nghiệp học viên: " + e.getMessage()));
        }
    }
    
    /**
     * Đồng bộ trạng thái Student từ Enrollment
     * POST /api/status-management/student/{studentId}/sync
     */
    @PostMapping("/student/{studentId}/sync")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'STAFF')")
    public ResponseEntity<Map<String, String>> syncStudentStatus(@PathVariable Integer studentId) {
        
        try {
            statusManagementService.syncStudentStatusFromEnrollments(studentId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Đã đồng bộ trạng thái student",
                "studentId", studentId.toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Lỗi khi đồng bộ trạng thái: " + e.getMessage()));
        }
    }
}
