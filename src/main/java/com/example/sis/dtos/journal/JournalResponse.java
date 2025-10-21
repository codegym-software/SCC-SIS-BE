package com.example.sis.dtos.journal;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO response cho nhật ký lớp học
 */
public class JournalResponse {

    private Integer journalId;
    private Integer classId;
    private String className; // Tên lớp
    private Integer teacherId;
    private String teacherName; // Tên giảng viên
    private String title;
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate journalDate;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime journalTime;
    
    private String journalType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters & Setters

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
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

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

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

    public LocalTime getJournalTime() {
        return journalTime;
    }

    public void setJournalTime(LocalTime journalTime) {
        this.journalTime = journalTime;
    }

    public String getJournalType() {
        return journalType;
    }

    public void setJournalType(String journalType) {
        this.journalType = journalType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
