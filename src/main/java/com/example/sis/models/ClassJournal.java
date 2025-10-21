package com.example.sis.models;

import com.example.sis.enums.JournalType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity cho bảng class_journals (Nhật ký lớp học)
 */
@Entity
@Table(name = "class_journals",
        indexes = {
                @Index(name = "idx_journals_class_date_id", 
                       columnList = "class_id, journal_date DESC, journal_id DESC"),
                @Index(name = "idx_journals_teacher_date_id", 
                       columnList = "teacher_id, journal_date DESC, journal_id DESC"),
                @Index(name = "idx_journals_class_type_date", 
                       columnList = "class_id, journal_type, journal_date DESC"),
                @Index(name = "idx_journals_deleted_at", 
                       columnList = "deleted_at"),
                @Index(name = "idx_journals_class_notdeleted_date", 
                       columnList = "class_id, deleted_at, journal_date DESC")
        })
public class ClassJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_id")
    private Integer journalId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_class_journals_class"))
    private ClassEntity classEntity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_class_journals_teacher"))
    private User teacher;

    @NotBlank
    @Size(min = 3, max = 500)
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @NotBlank
    @Size(min = 10)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(name = "journal_time")
    private LocalTime journalTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 20)
    private JournalType journalType = JournalType.NOTE;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", 
                foreignKey = @ForeignKey(name = "fk_class_journals_created_by"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", 
                foreignKey = @ForeignKey(name = "fk_class_journals_updated_by"))
    private User updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getJournalDate() {
        return journalDate;
    }

    public void setJournalDate(LocalDate journalDate) {
        this.journalDate = journalDate;
    }

    public LocalTime getJournalTime() {
        return journalTime;
    }

    public void setJournalTime(LocalTime journalTime) {
        this.journalTime = journalTime;
    }

    public JournalType getJournalType() {
        return journalType;
    }

    public void setJournalType(JournalType journalType) {
        this.journalType = journalType;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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
