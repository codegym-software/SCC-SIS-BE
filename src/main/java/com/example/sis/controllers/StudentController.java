package com.example.sis.controllers;

import com.example.sis.dtos.student.CreateStudentRequest;
import com.example.sis.dtos.student.StudentResponse;
import com.example.sis.dtos.student.UpdateStudentRequest;
import com.example.sis.dtos.student.UpdateStudentStatusRequest;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;

/**
 * Controller quản lý hồ sơ học viên
 * Base path: /api/students
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final UserRoleRepository userRoleRepository;

    public StudentController(StudentService studentService, UserRoleRepository userRoleRepository) {
        this.studentService = studentService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Tạo hồ sơ học viên mới
     * - Super Admin: có thể tạo học viên
     * - Academic Staff: có thể tạo học viên
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            Authentication authentication) {

        Integer createdByUserId = getCurrentUserId(authentication);
        if (createdByUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StudentResponse response = studentService.createStudent(request, createdByUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Lấy danh sách tất cả học viên
     * - Super Admin: xem tất cả
     * - Academic Staff: xem tất cả
     */
    @GetMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        List<StudentResponse> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    /**
     * Export students to Excel (.xlsx)
     */
    @GetMapping("/export")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<byte[]> exportStudents() {
        try {
            byte[] data = studentService.exportStudentsToExcel();

            String filename = "students.xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thông tin chi tiết học viên theo ID
     * - Super Admin: xem chi tiết
     * - Academic Staff: xem chi tiết
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Integer id) {
        try {
            StudentResponse student = studentService.getStudentById(id);
            return ResponseEntity.ok(student);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Cập nhật thông tin học viên
     * - Super Admin: có thể cập nhật
     * - Academic Staff: có thể cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateStudentRequest request,
            Authentication authentication) {

        Integer updatedByUserId = getCurrentUserId(authentication);
        if (updatedByUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StudentResponse response = studentService.updateStudent(id, request, updatedByUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Xóa mềm học viên (soft delete)
     * - Super Admin: có thể xóa
     * - Academic Staff: có thể xóa
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<Void> softDeleteStudent(@PathVariable Integer id) {
        try {
            studentService.softDeleteStudent(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Tìm kiếm học viên theo tên hoặc email
     * - Super Admin: có thể tìm kiếm
     * - Academic Staff: có thể tìm kiếm
     */
    @GetMapping("/search")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<List<StudentResponse>> searchStudents(
            @RequestParam(required = false) String keyword) {
        List<StudentResponse> students = studentService.searchStudents(keyword);
        return ResponseEntity.ok(students);
    }

    /**
     * Import students from uploaded Excel (.xlsx) file
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<List<StudentResponse>> importStudents(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Integer createdByUserId = getCurrentUserId(authentication);
        if (createdByUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<StudentResponse> created = studentService.importStudentsFromExcel(file, createdByUserId);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cập nhật trạng thái học viên
     * - Super Admin: có thể cập nhật
     * - Academic Staff: có thể cập nhật
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<StudentResponse> updateStudentStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateStudentStatusRequest request,
            Authentication authentication) {

        Integer updatedByUserId = getCurrentUserId(authentication);
        if (updatedByUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StudentResponse response = studentService.updateStudentStatus(id, request.getStatus(), updatedByUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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
}
