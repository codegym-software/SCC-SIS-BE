package com.example.sis.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "programs",
        indexes = {
                @Index(name = "idx_programs_is_active", columnList = "is_active"),
                @Index(name = "idx_programs_deleted_at", columnList = "deleted_at"),
                @Index(name = "idx_programs_name", columnList = "name"),
                @Index(name = "idx_programs_category", columnList = "category_code"),
                @Index(name = "idx_programs_delivery", columnList = "delivery_mode")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_programs_code", columnNames = "code")
        })
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Integer programId;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 10)
    private DeliveryMode deliveryMode = DeliveryMode.OFFLINE;

    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_programs_created_by"))
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_programs_updated_by"))
    private User updatedBy;

    public enum DeliveryMode {
        OFFLINE, ONLINE, HYBRID
    }

    // ===== Getter & Setter =====
    public Integer getProgramId() { return programId; }
    public void setProgramId(Integer programId) { this.programId = programId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
}
