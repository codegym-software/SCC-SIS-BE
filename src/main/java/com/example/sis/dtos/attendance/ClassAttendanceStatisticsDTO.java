package com.example.sis.dtos.attendance;

import java.util.List;

public class ClassAttendanceStatisticsDTO {
    private Integer classId;
    private String className;
    private Integer month;
    private Integer year;
    private Integer totalStudents;
    private Integer totalSessions;
    private Double averageAttendanceRate;
    private Integer studentsNeedingHelp;
    private List<DailyAttendanceDTO> dailyAttendance;
    private List<StudentStatisticDTO> studentStatistics;

    public ClassAttendanceStatisticsDTO() {
    }

    public ClassAttendanceStatisticsDTO(Integer classId, String className, Integer month, Integer year,
                                       Integer totalStudents, Integer totalSessions, 
                                       Double averageAttendanceRate, Integer studentsNeedingHelp,
                                       List<DailyAttendanceDTO> dailyAttendance,
                                       List<StudentStatisticDTO> studentStatistics) {
        this.classId = classId;
        this.className = className;
        this.month = month;
        this.year = year;
        this.totalStudents = totalStudents;
        this.totalSessions = totalSessions;
        this.averageAttendanceRate = averageAttendanceRate;
        this.studentsNeedingHelp = studentsNeedingHelp;
        this.dailyAttendance = dailyAttendance;
        this.studentStatistics = studentStatistics;
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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Double getAverageAttendanceRate() {
        return averageAttendanceRate;
    }

    public void setAverageAttendanceRate(Double averageAttendanceRate) {
        this.averageAttendanceRate = averageAttendanceRate;
    }

    public Integer getStudentsNeedingHelp() {
        return studentsNeedingHelp;
    }

    public void setStudentsNeedingHelp(Integer studentsNeedingHelp) {
        this.studentsNeedingHelp = studentsNeedingHelp;
    }

    public List<DailyAttendanceDTO> getDailyAttendance() {
        return dailyAttendance;
    }

    public void setDailyAttendance(List<DailyAttendanceDTO> dailyAttendance) {
        this.dailyAttendance = dailyAttendance;
    }

    public List<StudentStatisticDTO> getStudentStatistics() {
        return studentStatistics;
    }

    public void setStudentStatistics(List<StudentStatisticDTO> studentStatistics) {
        this.studentStatistics = studentStatistics;
    }
}
