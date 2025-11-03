package com.example.sis.dtos.grade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO để tạo đợt nhập điểm mới
 */
public class CreateGradeEntryRequest {

    @NotNull(message = "classId is required")
    private Integer classId;

    @NotNull(message = "moduleId is required")
    private Integer moduleId;

    @NotNull(message = "entryDate is required")
    private LocalDate entryDate;

    @NotEmpty(message = "gradeRecords must not be empty")
    @Valid
    private List<GradeRecordRequest> gradeRecords;

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public List<GradeRecordRequest> getGradeRecords() {
        return gradeRecords;
    }

    public void setGradeRecords(List<GradeRecordRequest> gradeRecords) {
        this.gradeRecords = gradeRecords;
    }
}

