package com.example.sis.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "centers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_centers_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_centers_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_centers_code", columnList = "code"),
                @Index(name = "idx_centers_name", columnList = "name")
        })
public class Center {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "center_id")
    private Integer centerId;

    @Column(name = "code", nullable = false, length = 32)
    @Comment("Mã ngắn (VD: HN, HCM)")
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Center() {}

    // Getters & Setters
    public Integer getCenterId() { return centerId; }
    public void setCenterId(Integer centerId) { this.centerId = centerId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
