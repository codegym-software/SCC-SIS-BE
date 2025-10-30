package com.example.sis.dtos.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về thông tin buổi điểm danh
 */
public class AttendanceSessionResponse {

    private Integer sessionId;
    private Integer classId;
    private String className;
    private Integer teacherId;
    private String teacherName;
    private LocalDate attendanceDate;
    private String notes;
    private Integer totalStudents;
    private Integer presentCount;
    private Integer absentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttendanceRecordResponse> records;

    // Getters and Setters
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Integer getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(Integer presentCount) {
        this.presentCount = presentCount;
    }

    public Integer getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(Integer absentCount) {
        this.absentCount = absentCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<AttendanceRecordResponse> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecordResponse> records) {
        this.records = records;
    }
}

