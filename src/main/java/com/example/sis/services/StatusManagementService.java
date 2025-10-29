package com.example.sis.services;

import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.enums.OverallStatus;

/**
 * Service quản lý logic chuyển đổi trạng thái Student và Enrollment
 */
public interface StatusManagementService {
    
    /**
     * Chuyển đổi trạng thái Enrollment và đồng bộ với Student
     * 
     * @param enrollmentId ID của enrollment
     * @param newStatus Trạng thái mới
     * @param updatedBy ID người thực hiện
     * @param note Ghi chú (optional)
     */
    void changeEnrollmentStatus(Integer enrollmentId, EnrollmentStatus newStatus, Integer updatedBy, String note);
    
    /**
     * Chuyển đổi trạng thái Student (chỉ áp dụng cho các trường hợp đặc biệt)
     * 
     * @param studentId ID của student
     * @param newStatus Trạng thái mới
     * @param updatedBy ID người thực hiện
     * @param note Ghi chú (optional)
     */
    void changeStudentStatus(Integer studentId, OverallStatus newStatus, Integer updatedBy, String note);
    
    /**
     * Tự động chuyển tất cả học viên trong lớp thành tốt nghiệp khi lớp hoàn thành
     * 
     * @param classId ID của lớp học
     * @param updatedBy ID người thực hiện
     */
    void autoGraduateClassStudents(Integer classId, Integer updatedBy);
    
    /**
     * Đồng bộ trạng thái Student dựa trên các Enrollment hiện tại
     * 
     * @param studentId ID của student
     */
    void syncStudentStatusFromEnrollments(Integer studentId);
    
    /**
     * Lấy trạng thái Student được tính từ các Enrollment
     * 
     * @param studentId ID của student
     * @return Trạng thái được tính toán
     */
    OverallStatus calculateStudentStatusFromEnrollments(Integer studentId);
}
