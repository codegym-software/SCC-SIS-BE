package com.example.sis.dtos.classteacher;

import java.time.LocalDateTime;

public class ClassLecturerResponse {
    private Integer id;
    private Integer classId;
    private Integer teacherId;
    private String teacherName;
    private String teacherEmail;
    private LocalDateTime effStartDate;
    private LocalDateTime effEndDate;
    private LocalDateTime createdAt;
    private String createdBy;

    public ClassLecturerResponse() {
    }

    public ClassLecturerResponse(Integer id, Integer classId, Integer teacherId, String teacherName,
            String teacherEmail, LocalDateTime effStartDate, LocalDateTime effEndDate,
            LocalDateTime createdAt, String createdBy) {
        this.id = id;
        this.classId = classId;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.teacherEmail = teacherEmail;
        this.effStartDate = effStartDate;
        this.effEndDate = effEndDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherEmail() {
        return teacherEmail;
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public LocalDateTime getEffStartDate() {
        return effStartDate;
    }

    public void setEffStartDate(LocalDateTime effStartDate) {
        this.effStartDate = effStartDate;
    }

    public LocalDateTime getEffEndDate() {
        return effEndDate;
    }

    public void setEffEndDate(LocalDateTime effEndDate) {
        this.effEndDate = effEndDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}