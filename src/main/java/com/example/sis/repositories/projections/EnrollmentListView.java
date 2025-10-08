package com.example.sis.repositories.projections;

import com.example.sis.enums.EnrollmentStatus;
import java.time.LocalDate;

/**
 * Projection cho danh sách học viên của lớp.
 * Dữ liệu đủ để hiển thị list FE, không load quan hệ thừa.
 */
public interface EnrollmentListView {
    Integer getEnrollmentId();
    Integer getClassId();
    Integer getStudentId();
    String getStudentName();
    String getStudentEmail();
    EnrollmentStatus getStatus();
    LocalDate getEnrolledAt();
    LocalDate getLeftAt();
    String getNote();
}
