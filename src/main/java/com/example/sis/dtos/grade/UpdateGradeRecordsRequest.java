package com.example.sis.dtos.grade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO để sửa điểm học viên trong một đợt nhập điểm
 */
public class UpdateGradeRecordsRequest {

    @NotNull(message = "classId is required")
    private Integer classId;

    @NotNull(message = "moduleId is required")
    private Integer moduleId;

    @NotNull(message = "semester is required")
    private Integer semester;

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

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
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
