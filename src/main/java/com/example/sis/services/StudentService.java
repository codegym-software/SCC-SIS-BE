package com.example.sis.services;

import com.example.sis.dtos.student.CreateStudentRequest;
import com.example.sis.dtos.student.StudentResponse;
import com.example.sis.dtos.student.StudentWithEnrollmentsResponse;
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
     * Lấy thông tin chi tiết học viên với enrollments theo ID
     */
    StudentWithEnrollmentsResponse getStudentWithEnrollmentsById(Integer studentId);

    /**
     * Lấy danh sách tất cả học viên với enrollments chi tiết
     */
    List<StudentWithEnrollmentsResponse> getAllStudentsWithEnrollments();

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
     * Export students to an Excel (.xlsx) file as bytes
     * @param status (Optional) Filter by status: STUDYING, GRADUATED, SUSPENDED, ON_LEAVE
     */
    byte[] exportStudentsToExcel(String status) throws IOException;

    /**
     * Generate Excel template for student import
     */
    byte[] generateImportTemplate() throws IOException;

    /**
     * Import students from uploaded Excel file. Returns list of created/parsed StudentResponse.
     * createdByUserId is used to set audit fields for created records.
     */
    List<StudentResponse> importStudentsFromExcel(MultipartFile file, Integer createdByUserId) throws IOException;
    /**
     * Cập nhật trạng thái học viên
     */
    StudentResponse updateStudentStatus(Integer studentId, String status, Integer updatedByUserId);

    /**
     * Lấy danh sách tất cả cảnh báo học viên theo trung tâm
     * @param centerId ID của trung tâm (optional, null = tất cả)
     * @return Danh sách tất cả học viên có cảnh báo
     */
    List<java.util.Map<String, Object>> getAllStudentWarnings(Integer centerId);

    /**
     * Lấy danh sách cảnh báo của học viên hiện tại
     * @param userId ID của user hiện tại
     * @return Danh sách cảnh báo (vắng > 2, trượt > 2)
     */
    List<java.util.Map<String, Object>> getStudentWarnings(Integer userId);
}
