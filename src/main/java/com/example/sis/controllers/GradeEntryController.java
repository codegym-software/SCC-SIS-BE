package com.example.sis.controllers;

import com.example.sis.dtos.grade.CreateGradeEntryRequest;
import com.example.sis.dtos.grade.GradeEntryDetailResponse;
import com.example.sis.dtos.grade.GradeEntryResponse;
import com.example.sis.dtos.grade.StudentGradesResponse;
import com.example.sis.dtos.grade.UpdateGradeRecordsRequest;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.securities.AuthzService;
import com.example.sis.services.GradeEntryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller cho quản lý đợt nhập điểm
 * Phân quyền: LECTURER (giảng viên) được phân công vào lớp
 */
@RestController
@RequestMapping("/api/grade-entries")
public class GradeEntryController {

    private final GradeEntryService gradeEntryService;
    private final UserRoleRepository userRoleRepository;
    private final AuthzService authzService;

    public GradeEntryController(
            GradeEntryService gradeEntryService,
            UserRoleRepository userRoleRepository,
            AuthzService authzService) {
        this.gradeEntryService = gradeEntryService;
        this.userRoleRepository = userRoleRepository;
        this.authzService = authzService;
    }

    /**
     * POST /api/grade-entries
     * Tạo đợt nhập điểm mới với danh sách điểm của học viên
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @PostMapping
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #request.classId)")
    public ResponseEntity<GradeEntryDetailResponse> createGradeEntry(
            @Valid @RequestBody CreateGradeEntryRequest request,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GradeEntryDetailResponse response = gradeEntryService.createGradeEntry(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/grade-entries
     * Lấy danh sách đợt nhập điểm của một lớp
     * Có thể filter theo moduleId và entryDate
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @GetMapping
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<List<GradeEntryResponse>> getGradeEntries(
            @RequestParam(required = true) Integer classId,
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) LocalDate entryDate) {
        List<GradeEntryResponse> responses = gradeEntryService.getGradeEntriesByClass(
                classId, moduleId, entryDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/grade-entries/student-grades
     * Lấy điểm của học viên theo lớp, semester và module
     * - Nếu chưa có moduleId: trả về danh sách modules để chọn
     * - Nếu có moduleId: trả về danh sách điểm của học viên trong lớp, cùng moduleId
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @GetMapping("/student-grades")
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<StudentGradesResponse> getStudentGrades(
            @RequestParam(required = true) Integer classId,
            @RequestParam(required = true) Integer semester,
            @RequestParam(required = false) Integer moduleId) {
        StudentGradesResponse response = 
                gradeEntryService.getStudentGrades(classId, semester, moduleId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/grade-entries
     * Xóa đợt nhập điểm theo classId, moduleId và entryDate
     * Sẽ xóa cả grade_entry và tất cả grade_records trong đợt đó (cascade)
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @DeleteMapping
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<Void> deleteGradeEntry(
            @RequestParam(required = true) Integer classId,
            @RequestParam(required = true) Integer moduleId,
            @RequestParam(required = true) LocalDate entryDate) {
        gradeEntryService.deleteGradeEntry(classId, moduleId, entryDate);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/grade-entries
     * Sửa điểm học viên trong một đợt nhập điểm
     * Cập nhật các grade_records trong grade_entry được xác định bởi classId, moduleId, entryDate
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @PutMapping
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #request.classId)")
    public ResponseEntity<GradeEntryDetailResponse> updateGradeRecords(
            @Valid @RequestBody UpdateGradeRecordsRequest request,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GradeEntryDetailResponse response = gradeEntryService.updateGradeRecords(request, currentUserId);
        return ResponseEntity.ok(response);
    }

    // ===== Helper methods =====

    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }
}

