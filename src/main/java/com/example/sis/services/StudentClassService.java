package com.example.sis.services;

import com.example.sis.dtos.classes.ClassResponse;

import java.util.List;

/**
 * Service để lấy danh sách lớp học của học viên
 */
public interface StudentClassService {
    
    /**
     * Lấy danh sách lớp học mà học viên đang/đã tham gia
     * @param studentId ID của học viên
     * @return Danh sách ClassResponse
     */
    List<ClassResponse> getClassesByStudentId(Integer studentId);
}

