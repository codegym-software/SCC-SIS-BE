package com.example.sis.dtos.classteacher;

import jakarta.validation.constraints.NotNull;

public class RemoveLecturerRequest {
    @NotNull(message = "Lecturer ID is required")
    private Integer lecturerId;

    public RemoveLecturerRequest() {
    }

    public RemoveLecturerRequest(Integer lecturerId) {
        this.lecturerId = lecturerId;
    }

    public Integer getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(Integer lecturerId) {
        this.lecturerId = lecturerId;
    }
}