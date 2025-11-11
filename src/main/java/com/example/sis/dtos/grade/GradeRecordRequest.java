package com.example.sis.dtos.grade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * DTO cho mỗi bản ghi điểm trong request tạo grade entry
 */
public class GradeRecordRequest {

    @NotNull(message = "studentId is required")
    private Integer studentId;

    @DecimalMin(value = "0.0", message = "theoryScore must be >= 0")
    @DecimalMax(value = "10.0", message = "theoryScore must be <= 10")
    private BigDecimal theoryScore;

    @DecimalMin(value = "0.0", message = "practiceScore must be >= 0")
    @DecimalMax(value = "10.0", message = "practiceScore must be <= 10")
    private BigDecimal practiceScore;

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
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
}

