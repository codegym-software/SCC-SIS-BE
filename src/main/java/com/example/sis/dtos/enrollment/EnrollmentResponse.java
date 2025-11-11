package com.example.sis.dtos.enrollment;

import java.time.LocalDate;

/**
 * DTO trả về cho FE (list học viên của lớp / chi tiết ghi danh).
 * Gọn nhẹ, chỉ những field cần thiết.
 */
public class EnrollmentResponse {

    private Integer enrollmentId;
    private Integer classId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private String studentOverallStatus;  // Trạng thái tổng quan của học viên (ACTIVE, DROPPED, etc.)
    private String status;  // Trạng thái enrollment trong lớp
    private LocalDate enrolledAt;
    private LocalDate leftAt;
    private String note;

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

    public String getStudentName() {
        return studentName;
    }
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentOverallStatus() {
        return studentOverallStatus;
    }
    public void setStudentOverallStatus(String studentOverallStatus) {
        this.studentOverallStatus = studentOverallStatus;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
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
