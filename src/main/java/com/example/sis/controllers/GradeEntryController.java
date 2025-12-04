package com.example.sis.controllers;

import com.example.sis.dtos.grade.CreateGradeEntryRequest;
import com.example.sis.dtos.grade.GradeEntryDetailResponse;
import com.example.sis.dtos.grade.GradeEntryResponse;
import com.example.sis.dtos.grade.GradeRecordResponse;
import com.example.sis.dtos.grade.StudentGradesResponse;
import com.example.sis.dtos.grade.UpdateGradeRecordsRequest;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.securities.AuthzService;
import com.example.sis.services.GradeEntryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * GET /api/grade-entries/my-grades
     * Lấy tất cả điểm thi của học viên hiện tại (dựa vào token)
     * Trả về danh sách điểm theo từng module
     * Phân quyền: STUDENT
     */
    @GetMapping("/my-grades")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<List<GradeRecordResponse>> getMyGrades(Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<GradeRecordResponse> grades = gradeEntryService.getMyGrades(currentUserId);
        return ResponseEntity.ok(grades);
    }

    /**
     * GET /api/grade-entries/student/{studentId}
     * Lấy tất cả điểm thi của một học viên cụ thể
     * Trả về danh sách điểm theo từng module
     * Phân quyền: LECTURER, ACADEMIC_STAFF, SUPER_ADMIN hoặc STUDENT (xem điểm của chính mình)
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'LECTURER', 'ACADEMIC_STAFF', 'SUPER_ADMIN', 'STUDENT')")
    public ResponseEntity<List<GradeRecordResponse>> getStudentGradesByStudentId(
            @PathVariable Integer studentId) {
        List<GradeRecordResponse> grades = gradeEntryService.getStudentGradesByStudentId(studentId);
        return ResponseEntity.ok(grades);
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

    /**
     * POST /api/grade-entries/import
     * Import điểm từ file Excel cho một đợt nhập điểm
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<GradeEntryDetailResponse> importGradesFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = true) Integer classId,
            @RequestParam(required = true) Integer moduleId,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GradeEntryDetailResponse response = gradeEntryService.importGradesFromExcel(
                file, classId, moduleId, entryDate, currentUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/grade-entries/export-template
     * Download Excel template để nhập điểm
     * Phân quyền: LECTURER được phân công vào lớp
     */
    @GetMapping("/export-template")
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<byte[]> downloadGradeTemplate(
            @RequestParam(required = true) Integer classId,
            @RequestParam(required = true) Integer moduleId) {
        try {
            byte[] data = gradeEntryService.generateGradeImportTemplate(classId, moduleId);
            String filename = "grade_import_template.xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/grade-entries/export
     * Export danh sách điểm ra Excel (sau khi filter)
     * Phân quyền: LECTURER được phân công vào lớp
     * @param entryDate Bắt buộc khi đã chọn moduleId
     */
    @GetMapping("/export")
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER') and @authz.hasAcademicAccessForClass(authentication, #classId)")
    public ResponseEntity<byte[]> exportGrades(
            @RequestParam(required = true) Integer classId,
            @RequestParam(required = true) Integer semester,
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate) {
        try {
            byte[] data = gradeEntryService.exportGradesToExcel(classId, semester, moduleId, entryDate);
            String filename = "grades_export.xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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