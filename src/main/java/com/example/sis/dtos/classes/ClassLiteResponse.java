package com.example.sis.dtos.classes;

import com.example.sis.models.ClassEntity;

public class ClassLiteResponse {
    private Integer classId;
    private String name;
    private String programName;
    private String centerName;
    private ClassEntity.ClassStatus status;

    // Constructors
    public ClassLiteResponse() {
    }

    public ClassLiteResponse(Integer classId, String name, String programName, String centerName,
            ClassEntity.ClassStatus status) {
        this.classId = classId;
        this.name = name;
        this.programName = programName;
        this.centerName = centerName;
        this.status = status;
    }

    // Getters and Setters
    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public ClassEntity.ClassStatus getStatus() {
        return status;
    }

    public void setStatus(ClassEntity.ClassStatus status) {
        this.status = status;
    }
}