package com.example.sis.dtos.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO để tạo buổi điểm danh mới
 */
public class CreateAttendanceSessionRequest {

    @NotNull(message = "classId is required")
    private Integer classId;

    @NotNull(message = "teacherId is required")
    private Integer teacherId;

    @NotNull(message = "attendanceDate is required")
    private LocalDate attendanceDate;

    private String notes;

    @NotNull(message = "records are required")
    @Size(min = 1, message = "At least one attendance record is required")
    private List<AttendanceRecordRequest> records;

    // Getters and Setters
    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
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

    public List<AttendanceRecordRequest> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecordRequest> records) {
        this.records = records;
    }

    /**
     * Inner class for attendance record request
     */
    public static class AttendanceRecordRequest {
        @NotNull(message = "enrollmentId is required")
        private Integer enrollmentId;

        @NotNull(message = "studentId is required")
        private Integer studentId;

        @NotBlank(message = "status is required")
        private String status;

        private String notes;

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
}

