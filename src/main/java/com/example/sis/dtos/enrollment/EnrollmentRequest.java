package com.example.sis.dtos.enrollment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO dùng khi thêm học viên vào lớp (ghi danh).
 */
public class EnrollmentRequest {

    @NotNull(message = "studentId is required")
    private Integer studentId;

    private LocalDate enrolledAt;
    private String note;

    public Integer getStudentId() {
        return studentId;
    }
    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public LocalDate getEnrolledAt() {
        return enrolledAt;
    }
    public void setEnrolledAt(LocalDate enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }
}
