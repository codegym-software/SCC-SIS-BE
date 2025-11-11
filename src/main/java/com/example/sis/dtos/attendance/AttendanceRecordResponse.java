package com.example.sis.dtos.attendance;

import com.example.sis.enums.AttendanceStatus;

/**
 * DTO trả về chi tiết điểm danh của một học viên
 */
public class AttendanceRecordResponse {

    private Integer recordId;
    private Integer sessionId;
    private Integer enrollmentId;
    private Integer studentId;
    private String studentName;
    private String studentCode;
    private String studentEmail;
    private String status;
    private String notes;

    // Constructors
    public AttendanceRecordResponse() {
    }

    public AttendanceRecordResponse(
            Integer recordId,
            Integer sessionId,
            Integer enrollmentId,
            Integer studentId,
            String studentName,
            String studentCode,
            String studentEmail,
            String status,
            String notes) {
        this.recordId = recordId;
        this.sessionId = sessionId;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentCode = studentCode;
        this.studentEmail = studentEmail;
        this.status = status;
        this.notes = notes;
    }

    // Constructor nhận AttendanceStatus enum (cho JPQL)
    public AttendanceRecordResponse(
            Integer recordId,
            Integer sessionId,
            Integer enrollmentId,
            Integer studentId,
            String studentName,
            String studentCode,
            String studentEmail,
            AttendanceStatus status,
            String notes) {
        this.recordId = recordId;
        this.sessionId = sessionId;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentCode = studentCode;
        this.studentEmail = studentEmail;
        this.status = status != null ? status.name() : null;
        this.notes = notes;
    }

    // Getters and Setters
    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
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

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

