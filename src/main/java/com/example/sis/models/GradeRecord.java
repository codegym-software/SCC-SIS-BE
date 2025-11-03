package com.example.sis.models;

import com.example.sis.enums.PassStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grade_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_grade_records_entry_student",
                        columnNames = {"grade_entry_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_grade_records_entry",
                        columnList = "grade_entry_id, student_id"),
                @Index(name = "idx_grade_records_student",
                        columnList = "student_id, grade_entry_id"),
                @Index(name = "idx_grade_records_pass_status",
                        columnList = "pass_status"),
                @Index(name = "idx_grade_records_entry_final",
                        columnList = "grade_entry_id, final_score")
        })
public class GradeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_record_id")
    private Integer gradeRecordId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_records_entry"))
    private GradeEntry gradeEntry;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_records_student"))
    private Student student;

    @Column(name = "theory_score", precision = 5, scale = 2)
    private BigDecimal theoryScore;

    @Column(name = "practice_score", precision = 5, scale = 2)
    private BigDecimal practiceScore;

    // Generated columns (read-only)
    @Column(name = "final_score", insertable = false, updatable = false)
    private BigDecimal finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_status", insertable = false, updatable = false)
    private PassStatus passStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====
    public Integer getGradeRecordId() {
        return gradeRecordId;
    }

    public void setGradeRecordId(Integer gradeRecordId) {
        this.gradeRecordId = gradeRecordId;
    }

    public GradeEntry getGradeEntry() {
        return gradeEntry;
    }

    public void setGradeEntry(GradeEntry gradeEntry) {
        this.gradeEntry = gradeEntry;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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

    public PassStatus getPassStatus() {
        return passStatus;
    }

    public void setPassStatus(PassStatus passStatus) {
        this.passStatus = passStatus;
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
}

