package com.example.sis.dtos.enrollment;

import com.example.sis.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class EnrollmentDTO {

    private Integer enrollmentId;

    @NotNull(message = "ID lớp học không được để trống")
    private Integer classId;

    @NotNull(message = "ID học viên không được để trống")
    private Integer studentId;

    @NotNull(message = "Trạng thái ghi danh không được để trống")
    private EnrollmentStatus status;

    @NotNull(message = "Ngày bắt đầu ghi danh không được để trống")
    @PastOrPresent(message = "Ngày bắt đầu ghi danh phải là quá khứ hoặc hiện tại")
    private LocalDate enrolledAt;

    @PastOrPresent(message = "Ngày kết thúc ghi danh phải là quá khứ hoặc hiện tại")
    private LocalDate leftAt;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    // Constructors
    public EnrollmentDTO() {
    }

    // Getters and Setters
    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public LocalDate getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDate enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDate getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDate leftAt) {
        this.leftAt = leftAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}