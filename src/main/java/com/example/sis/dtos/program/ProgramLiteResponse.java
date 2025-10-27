package com.example.sis.dtos.program;

import com.example.sis.models.Program;

public class ProgramLiteResponse {
    private Integer programId;
    private String code;
    private String name;
    private String description;
    private Integer durationHours;
    private Program.DeliveryMode deliveryMode;
    private String categoryCode;
    private Boolean isActive;
    private Long moduleCount; // Số lượng modules trong program

    // Constructors
    public ProgramLiteResponse() {
    }

    public ProgramLiteResponse(Integer programId, String code, String name, String description,
            Integer durationHours, Program.DeliveryMode deliveryMode, String categoryCode,
            Boolean isActive, Long moduleCount) {
        this.programId = programId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.durationHours = durationHours;
        this.deliveryMode = deliveryMode;
        this.categoryCode = categoryCode;
        this.isActive = isActive;
        this.moduleCount = moduleCount;
    }

    // Getters and Setters
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

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Program.DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(Program.DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getModuleCount() {
        return moduleCount;
    }

    public void setModuleCount(Long moduleCount) {
        this.moduleCount = moduleCount;
    }
}