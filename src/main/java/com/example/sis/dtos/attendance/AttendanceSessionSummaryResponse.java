package com.example.sis.dtos.attendance;

import java.time.LocalDate;

/**
 * DTO trả về tóm tắt buổi điểm danh (dùng cho danh sách)
 */
public class AttendanceSessionSummaryResponse {

    private Integer sessionId;
    private LocalDate attendanceDate;
    private Integer totalStudents;
    private Integer presentCount;
    private Integer absentCount;

    // Getters and Setters
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
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
}

