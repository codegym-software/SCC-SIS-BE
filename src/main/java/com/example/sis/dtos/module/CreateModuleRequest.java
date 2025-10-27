package com.example.sis.dtos.module;

import jakarta.validation.constraints.*;

public class CreateModuleRequest {

    @NotNull(message = "program_id không được null")
    @Positive(message = "program_id phải là số dương")
    private Integer programId;

    @NotBlank(message = "Mã module không được để trống")
    @Size(max = 50, message = "Mã module không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã module chỉ chứa chữ IN HOA, số, dấu gạch ngang và gạch dưới")
    private String code;

    @NotBlank(message = "Tên module không được để trống")
    @Size(max = 255, message = "Tên module không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    // sequenceOrder là OPTIONAL - Nếu không điền, hệ thống tự động lấy max + 1 theo programId
    @Positive(message = "Thứ tự môn học phải là số dương")
    private Integer sequenceOrder;

    // semester sẽ được tự động tính từ sequenceOrder, không cần client gửi lên
    // Quy tắc: 1-6 -> sem 1, 7-13 -> sem 2, 14-20 -> sem 3, 21+ -> sem 4
    // Field này vẫn giữ để backward compatible, nhưng sẽ bị override bởi logic auto-calculate
    private Integer semester;

    @NotNull(message = "Số tín chỉ không được null")
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

    // ===== Getters & Setters =====
    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

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

    public Integer getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
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
}


