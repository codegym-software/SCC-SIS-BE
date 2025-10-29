package com.example.sis.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_teachers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ct_class_teacher_start", columnNames = {"class_id", "teacher_id", "start_date"})
        },
        indexes = {
                @Index(name = "idx_ct_class_effend_start_id", columnList = "class_id,eff_end_date,start_date,class_teacher_id"),
                @Index(name = "idx_ct_class_start_id", columnList = "class_id,start_date,class_teacher_id"),
                @Index(name = "idx_ct_teacher_start_id", columnList = "teacher_id,start_date,class_teacher_id"),
                @Index(name = "idx_ct_class_teacher_range", columnList = "class_id,teacher_id,start_date,end_date")
        })
public class ClassTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_teacher_id")
    private Integer classTeacherId;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_teachers_class", value = ConstraintMode.CONSTRAINT,
                    foreignKeyDefinition = "FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE"))
    private ClassEntity classEntity;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_teachers_teacher", value = ConstraintMode.CONSTRAINT,
                    foreignKeyDefinition = "FOREIGN KEY (teacher_id) REFERENCES users(user_id) ON DELETE RESTRICT"))
    private User teacher;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "eff_end_date", insertable = false, updatable = false)
    private LocalDate effEndDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "assigned_by",
            foreignKey = @ForeignKey(name = "fk_ct_assigned_by", value = ConstraintMode.CONSTRAINT,
                    foreignKeyDefinition = "FOREIGN KEY (assigned_by) REFERENCES users(user_id) ON DELETE SET NULL"))
    private User assignedBy;

    @ManyToOne
    @JoinColumn(name = "revoked_by",
            foreignKey = @ForeignKey(name = "fk_ct_revoked_by", value = ConstraintMode.CONSTRAINT,
                    foreignKeyDefinition = "FOREIGN KEY (revoked_by) REFERENCES users(user_id) ON DELETE SET NULL"))
    private User revokedBy;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    public ClassTeacher() {}

    // Getters & Setters
    public Integer getClassTeacherId() { return classTeacherId; }
    public void setClassTeacherId(Integer classTeacherId) { this.classTeacherId = classTeacherId; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getEffEndDate() { return effEndDate; }
    public void setEffEndDate(LocalDate effEndDate) { this.effEndDate = effEndDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public User getRevokedBy() { return revokedBy; }
    public void setRevokedBy(User revokedBy) { this.revokedBy = revokedBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    // Helper getter để JPQL query có thể truy cập teacher_id trực tiếp
    public Integer getTeacherId() {
        return teacher != null ? teacher.getUserId() : null;
    }
}