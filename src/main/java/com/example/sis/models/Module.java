package com.example.sis.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "modules",
        indexes = {
                @Index(name = "idx_modules_program", columnList = "program_id"),
                @Index(name = "idx_modules_is_active", columnList = "is_active"),
                @Index(name = "idx_modules_deleted_at", columnList = "deleted_at"),
                @Index(name = "idx_modules_name", columnList = "name"),
                @Index(name = "idx_modules_level", columnList = "level"),
                @Index(name = "idx_modules_sequence", columnList = "program_id, sequence_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_module_code_per_program", columnNames = {"program_id", "code"}),
                @UniqueConstraint(name = "uk_module_sequence_per_program", columnNames = {"program_id", "sequence_order"})
        })
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    private Integer moduleId;

    // ===== LIÊN KẾT VỚI PROGRAM =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false, foreignKey = @ForeignKey(name = "fk_modules_program"))
    private Program program;

    @Column(name = "program_id", insertable = false, updatable = false)
    private Integer programId;

    // ===== ĐỊNH DANH MÔN HỌC =====
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ===== SẮP XẾP TRONG KHÓA HỌC =====
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "semester")
    private Integer semester;

    // ===== THÔNG TIN HỌC TẬP =====
    @Column(name = "credits", nullable = false)
    private Integer credits;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;

    // ===== ĐỀ CƯƠNG (SYLLABUS) =====
    // JSON array: [{"url":"...","fileName":"...","fileType":"...","fileSize":123,"uploadedAt":"...","uploadedBy":1}]
    @Column(name = "syllabus_url", columnDefinition = "TEXT")
    private String syllabusUrl;

    @Column(name = "has_syllabus", nullable = false)
    private Boolean hasSyllabus = false;

    // ===== GHI CHÚ =====
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ===== TRẠNG THÁI & SOFT DELETE =====
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== AUDIT =====
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_modules_created_by"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_modules_updated_by"))
    private User updatedBy;

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
    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public String getSyllabusUrl() {
        return syllabusUrl;
    }

    public void setSyllabusUrl(String syllabusUrl) {
        this.syllabusUrl = syllabusUrl;
    }

    public Boolean getHasSyllabus() {
        return hasSyllabus;
    }

    public void setHasSyllabus(Boolean hasSyllabus) {
        this.hasSyllabus = hasSyllabus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}


