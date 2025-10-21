package com.example.sis.dtos.journal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO để cập nhật nhật ký lớp học
 */
public class UpdateJournalRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 3, max = 500, message = "Tiêu đề phải từ 3 đến 500 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 10, message = "Nội dung phải có ít nhất 10 ký tự")
    private String content;

    @NotNull(message = "Ngày nhật ký không được để trống")
    private LocalDate journalDate;

    private String journalType; // PROGRESS, ANNOUNCEMENT, ISSUE, NOTE, OTHER

    // Getters & Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getJournalDate() {
        return journalDate;
    }

    public void setJournalDate(LocalDate journalDate) {
        this.journalDate = journalDate;
    }

    public String getJournalType() {
        return journalType;
    }

    public void setJournalType(String journalType) {
        this.journalType = journalType;
    }
}
