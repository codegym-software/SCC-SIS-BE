package com.example.sis.dtos.classteacher;

import java.time.LocalDate;

public class RemoveLecturerRequest {
    private LocalDate endDate;
    private String note;

    public RemoveLecturerRequest() {
    }

    public RemoveLecturerRequest(LocalDate endDate, String note) {
        this.endDate = endDate;
        this.note = note;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}