package com.example.sis.dtos.attendance;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO để cập nhật buổi điểm danh
 */
public class UpdateAttendanceSessionRequest {

    private String notes;

    @Size(min = 1, message = "At least one attendance record is required")
    private List<UpdateAttendanceRecordRequest> records;

    // Getters and Setters
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<UpdateAttendanceRecordRequest> getRecords() {
        return records;
    }

    public void setRecords(List<UpdateAttendanceRecordRequest> records) {
        this.records = records;
    }

    /**
     * Inner class for updating attendance record
     */
    public static class UpdateAttendanceRecordRequest {
        @jakarta.validation.constraints.NotNull(message = "recordId is required")
        private Integer recordId;

        @jakarta.validation.constraints.NotBlank(message = "status is required")
        private String status;

        private String notes;

        public Integer getRecordId() {
            return recordId;
        }

        public void setRecordId(Integer recordId) {
            this.recordId = recordId;
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

