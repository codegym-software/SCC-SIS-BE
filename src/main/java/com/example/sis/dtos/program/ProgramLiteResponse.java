package com.example.sis.dtos.program;

import com.example.sis.models.Program;

public class ProgramLiteResponse {
    private Integer programId;
    private String code;
    private String name;
    private Integer durationHours;
    private Program.DeliveryMode deliveryMode;
    private String categoryCode;
    private String level;

    // Constructors
    public ProgramLiteResponse() {
    }

    public ProgramLiteResponse(Integer programId, String code, String name, Integer durationHours,
            Program.DeliveryMode deliveryMode, String categoryCode, String level) {
        this.programId = programId;
        this.code = code;
        this.name = name;
        this.durationHours = durationHours;
        this.deliveryMode = deliveryMode;
        this.categoryCode = categoryCode;
        this.level = level;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}