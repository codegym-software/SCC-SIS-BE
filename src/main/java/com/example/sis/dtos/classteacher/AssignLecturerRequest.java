package com.example.sis.dtos.classteacher;

import jakarta.validation.constraints.NotNull;

public class AssignLecturerRequest {
    @NotNull(message = "Lecturer ID is required")
    private Integer lecturerId;

    public AssignLecturerRequest() {
    }

    public AssignLecturerRequest(Integer lecturerId) {
        this.lecturerId = lecturerId;
    }

    public Integer getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(Integer lecturerId) {
        this.lecturerId = lecturerId;
    }
}