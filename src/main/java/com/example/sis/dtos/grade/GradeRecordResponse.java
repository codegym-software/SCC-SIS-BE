package com.example.sis.dtos.grade;

import java.math.BigDecimal;

/**
 * DTO trả về cho mỗi bản ghi điểm
 */
public class GradeRecordResponse {

    private Integer gradeRecordId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private BigDecimal theoryScore;
    private BigDecimal practiceScore;
    private BigDecimal finalScore;
    private String passStatus; // PASS or FAIL
    private String entryDate; // ISO date: "2025-01-15" - ngày thi của đợt nhập điểm này
    
    // Thông tin module và class (cho API lấy điểm theo student)
    private Integer moduleId;
    private String moduleCode;
    private String moduleName;
    private Integer semester;
    private Integer classId;
    private String className;

    public Integer getGradeRecordId() {
        return gradeRecordId;
    }

    public void setGradeRecordId(Integer gradeRecordId) {
        this.gradeRecordId = gradeRecordId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public BigDecimal getTheoryScore() {
        return theoryScore;
    }

    public void setTheoryScore(BigDecimal theoryScore) {
        this.theoryScore = theoryScore;
    }

    public BigDecimal getPracticeScore() {
        return practiceScore;
    }

    public void setPracticeScore(BigDecimal practiceScore) {
        this.practiceScore = practiceScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getPassStatus() {
        return passStatus;
    }

    public void setPassStatus(String passStatus) {
        this.passStatus = passStatus;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
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
}

