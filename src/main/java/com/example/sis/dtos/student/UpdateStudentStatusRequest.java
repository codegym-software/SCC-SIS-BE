package com.example.sis.dtos.student;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO cho việc cập nhật trạng thái học viên
 */
public class UpdateStudentStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // PENDING, ACTIVE, DROPPED, GRADUATED

    // Getters & Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

