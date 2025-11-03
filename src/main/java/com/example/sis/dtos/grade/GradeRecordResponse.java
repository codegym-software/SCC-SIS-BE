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
}

