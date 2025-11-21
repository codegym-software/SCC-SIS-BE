package com.example.sis.dtos.attendance;

import java.time.LocalDate;

public class DailyAttendanceDTO {
    private LocalDate date;
    private Integer sessionCount;
    private Integer presentCount;
    private Integer absentCount;
    private Double attendanceRate;

    public DailyAttendanceDTO() {
    }

    public DailyAttendanceDTO(LocalDate date, Integer sessionCount, Integer presentCount, 
                             Integer absentCount, Double attendanceRate) {
        this.date = date;
        this.sessionCount = sessionCount;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.attendanceRate = attendanceRate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(Integer sessionCount) {
        this.sessionCount = sessionCount;
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

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
