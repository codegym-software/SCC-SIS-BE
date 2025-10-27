package com.example.sis.dtos.module;

import jakarta.validation.constraints.*;

public class UpdateModuleRequest {

    @Size(max = 50, message = "Mã module không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã module chỉ chứa chữ IN HOA, số, dấu gạch ngang và gạch dưới")
    private String code;

    @Size(max = 255, message = "Tên module không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    // sequenceOrder và semester không cho phép sửa qua API này
    // Sử dụng endpoint PATCH /api/modules/reorder để thay đổi thứ tự

    @Min(value = 1, message = "Số tín chỉ phải từ 1-10")
    @Max(value = 10, message = "Số tín chỉ phải từ 1-10")
    private Integer credits;

    @Positive(message = "Số giờ học phải là số dương")
    private Integer durationHours;

    @Pattern(regexp = "^(Beginner|Intermediate|Advanced)$", 
             message = "Level phải là: Beginner, Intermediate hoặc Advanced")
    private String level;

    private Boolean isMandatory;

    private Boolean hasSyllabus;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;

    private Boolean isActive;

    // ===== Getters & Setters =====
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public Boolean getHasSyllabus() {
        return hasSyllabus;
    }

    public void setHasSyllabus(Boolean hasSyllabus) {
        this.hasSyllabus = hasSyllabus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}





