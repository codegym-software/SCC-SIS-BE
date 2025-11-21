package com.example.sis.dtos.attendance;

public class StudentStatisticDTO {
    private Integer studentId;
    private String studentName;
    private String studentCode;
    private Integer totalSessions;
    private Integer presentCount;
    private Integer absentCount;
    private Double attendanceRate;

    public StudentStatisticDTO() {
    }

    public StudentStatisticDTO(Integer studentId, String studentName, String studentCode,
                              Integer totalSessions, Integer presentCount, Integer absentCount,
                              Double attendanceRate) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentCode = studentCode;
        this.totalSessions = totalSessions;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.attendanceRate = attendanceRate;
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

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
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
