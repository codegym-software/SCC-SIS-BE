package com.example.sis.models;

import com.example.sis.enums.EnrollmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_enrollments_class_student_enrolled",
                        columnNames = {"class_id", "student_id", "enrolled_at"})
        },
        indexes = {
                @Index(name = "idx_enrollments_class_effend_start_id",
                        columnList = "class_id,eff_end_date,enrolled_at,enrollment_id"),
                @Index(name = "idx_enrollments_class_status",
                        columnList = "class_id,status"),
                @Index(name = "idx_enrollments_student_start_id",
                        columnList = "student_id,enrolled_at,enrollment_id")
        })
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Integer enrollmentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollments_class"))
    private ClassEntity classEntity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollments_student"))
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @NotNull
    @Column(name = "enrolled_at", nullable = false)
    private LocalDate enrolledAt;

    @Column(name = "left_at")
    private LocalDate leftAt;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", foreignKey = @ForeignKey(name = "fk_enrollments_assigned_by"))
    private User assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by", foreignKey = @ForeignKey(name = "fk_enrollments_revoked_by"))
    private User revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Cột sinh từ DB: COALESCE(left_at, '9999-12-31') — chỉ đọc */
    @Column(name = "eff_end_date", insertable = false, updatable = false)
    private LocalDate effectiveEndDate;

    @PrePersist
    void _prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (enrolledAt == null) enrolledAt = LocalDate.now();
        if (status == null) status = EnrollmentStatus.ACTIVE;
    }

    @PreUpdate
    void _preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper gom nghiệp vụ “xóa mềm/hủy ghi danh”.
     * - Set status=DROPPED
     * - leftAt = today nếu null/đang > today
     * - revokedBy/At
     * - Append note lý do (tuỳ chọn)
     */
    public void markRevoked(User actor, String reason) {
        LocalDate today = LocalDate.now();
        this.status = EnrollmentStatus.DROPPED;
        if (this.leftAt == null || this.leftAt.isAfter(today)) {
            this.leftAt = today;
        }
        if (actor != null) {
            this.revokedBy = actor;
        }
        this.revokedAt = LocalDateTime.now();
        if (reason != null && !reason.isBlank()) {
            String prefix = "[REVOKED " + today + "] ";
            this.note = (this.note == null || this.note.isBlank())
                    ? (prefix + reason.trim())
                    : (this.note + "\n" + prefix + reason.trim());
        }
    }

    // ===== Getters/Setters =====
    public Integer getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Integer enrollmentId) { this.enrollmentId = enrollmentId; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }

    public LocalDate getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDate enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDate getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDate leftAt) { this.leftAt = leftAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public User getRevokedBy() { return revokedBy; }
    public void setRevokedBy(User revokedBy) { this.revokedBy = revokedBy; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDate getEffectiveEndDate() { return effectiveEndDate; }
}
