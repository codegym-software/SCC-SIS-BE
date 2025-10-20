package com.example.sis.services;

import com.example.sis.dtos.student.CreateStudentRequest;
import com.example.sis.dtos.student.StudentResponse;
import com.example.sis.dtos.student.UpdateStudentRequest;

import java.util.List;

/**
 * Service quản lý hồ sơ học viên
 */
public interface StudentService {

    /**
     * Tạo hồ sơ học viên mới
     */
    StudentResponse createStudent(CreateStudentRequest request, Integer createdByUserId);

    /**
     * Lấy danh sách tất cả học viên
     */
    List<StudentResponse> getAllStudents();

    /**
     * Lấy thông tin chi tiết học viên theo ID
     */
    StudentResponse getStudentById(Integer studentId);

    /**
     * Cập nhật thông tin học viên
     */
    StudentResponse updateStudent(Integer studentId, UpdateStudentRequest request, Integer updatedByUserId);

    /**
     * Xóa mềm học viên (soft delete)
     */
    void softDeleteStudent(Integer studentId);

    /**
     * Tìm kiếm học viên theo tên hoặc email
     */
    List<StudentResponse> searchStudents(String keyword);

    /**
     * Cập nhật trạng thái học viên
     */
    StudentResponse updateStudentStatus(Integer studentId, String status, Integer updatedByUserId);
}
