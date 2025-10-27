package com.example.sis.services;

import com.example.sis.dtos.student.CreateStudentRequest;
import com.example.sis.dtos.student.StudentResponse;
import com.example.sis.dtos.student.UpdateStudentRequest;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

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

    /** Export all students to an Excel (.xlsx) file as bytes */
    byte[] exportStudentsToExcel() throws IOException;

    /**
     * Download template Excel file with headers only (no data rows)
     */
    byte[] downloadImportTemplate() throws IOException;

    /**
     * Import students from uploaded Excel file. Returns list of created/parsed StudentResponse.
     * createdByUserId is used to set audit fields for created records.
     */
    List<StudentResponse> importStudentsFromExcel(MultipartFile file, Integer createdByUserId) throws IOException;
    /**
     * Cập nhật trạng thái học viên
     */
    StudentResponse updateStudentStatus(Integer studentId, String status, Integer updatedByUserId);
}
