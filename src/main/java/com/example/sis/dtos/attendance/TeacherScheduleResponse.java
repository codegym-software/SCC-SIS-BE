package com.example.sis.dtos.attendance;

import java.time.LocalDate;

/**
 * DTO cho lịch dạy của giảng viên (API 1)
 */
public class TeacherScheduleResponse {

    private Integer classId;
    private String className;
    private LocalDate attendanceDate;
    private String sessionStatus; // "NOT_TAKEN" or "TAKEN"
    private String studyTime; // "MORNING", "AFTERNOON", or "EVENING"

    // Getters and Setters
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

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getStudyTime() {
        return studyTime;
    }

    public void setStudyTime(String studyTime) {
        this.studyTime = studyTime;
    }
}

