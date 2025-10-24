package com.example.sis.dtos.module;

import java.time.LocalDateTime;
import java.util.List;

public class ModuleResponse {
    
    private Integer moduleId;
    private Integer programId;
    private String programCode;
    private String programName;
    private String code;
    private String name;
    private String description;
    private Integer sequenceOrder;
    private Integer semester;
    private Integer credits;
    private Integer durationHours;
    private String level;
    private Boolean isMandatory;
    
    // DEPRECATED: Giữ để backward compatibility, nhưng không dùng nữa
    @Deprecated
    private String syllabusUrl;
    
    // NEW: Danh sách tài liệu học tập (thay thế syllabusUrl)
    private List<ModuleResourceDto> resources;
    
    private Boolean hasSyllabus;
    private String notes;
    private Boolean isActive;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer createdBy;
    private Integer updatedBy;

    // ===== Getters & Setters =====
    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
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

    @Deprecated
    public String getSyllabusUrl() {
        return syllabusUrl;
    }

    @Deprecated
    public void setSyllabusUrl(String syllabusUrl) {
        this.syllabusUrl = syllabusUrl;
    }

    public List<ModuleResourceDto> getResources() {
        return resources;
    }

    public void setResources(List<ModuleResourceDto> resources) {
        this.resources = resources;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }
}


