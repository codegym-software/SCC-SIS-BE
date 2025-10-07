package com.example.sis.dtos.classteacher;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AssignLecturerRequest {
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private String note;

    public AssignLecturerRequest() {
    }

    public AssignLecturerRequest(LocalDate startDate, String note) {
        this.startDate = startDate;
        this.note = note;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}