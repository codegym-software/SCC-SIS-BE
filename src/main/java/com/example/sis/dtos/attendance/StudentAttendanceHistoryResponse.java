package com.example.sis.dtos.attendance;

import com.example.sis.enums.AttendanceStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO trả về lịch sử điểm danh của học viên trong một lớp
 */
public class StudentAttendanceHistoryResponse {
    
    private Integer studentId;
    private String studentName;
    private String studentCode;
    private Integer classId;
    private String className;
    
    // Summary statistics
    private Integer totalSessions;
    private Integer presentCount;
    private Integer absentCount;
    
    // Detailed records
    private List<AttendanceDetail> records;

    // Getters and Setters
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

    public List<AttendanceDetail> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceDetail> records) {
        this.records = records;
    }

    /**
     * Inner class for attendance detail
     */
    public static class AttendanceDetail {
        private Integer sessionId;
        private LocalDate attendanceDate;
        private AttendanceStatus status;
        private String notes;
        private String teacherName;

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

        public AttendanceStatus getStatus() {
            return status;
        }

        public void setStatus(AttendanceStatus status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public void setTeacherName(String teacherName) {
            this.teacherName = teacherName;
        }
    }
}
