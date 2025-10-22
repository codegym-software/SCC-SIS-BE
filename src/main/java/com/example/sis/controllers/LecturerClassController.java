package com.example.sis.controllers;

import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.services.ClassTeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho giảng viên lấy danh sách lớp được gán
 * Endpoint: /api/lecturers/{teacherId}/classes
 */
@RestController
@RequestMapping("/api/lecturers")
public class LecturerClassController {

    private final ClassTeacherService classTeacherService;

    public LecturerClassController(ClassTeacherService classTeacherService) {
        this.classTeacherService = classTeacherService;
    }

    /**
     * Lấy danh sách lớp học mà giảng viên đang được gán
     * Chỉ giảng viên được xem lớp của mình, hoặc SUPER_ADMIN có thể xem tất cả
     */
    @GetMapping("/{teacherId}/classes")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.isOwnProfile(authentication, #teacherId)")
    public ResponseEntity<List<ClassResponse>> getClassesByTeacher(@PathVariable Integer teacherId) {
        List<ClassResponse> classes = classTeacherService.getClassesByTeacherId(teacherId);
        return ResponseEntity.ok(classes);
    }
}
