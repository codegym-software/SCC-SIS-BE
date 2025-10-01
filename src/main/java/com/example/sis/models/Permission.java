package com.example.sis.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_code", columnNames = "code")
}, indexes = {
        @Index(name = "idx_permissions_code", columnList = "code"),
        @Index(name = "idx_permissions_category", columnList = "category"),
        @Index(name = "idx_permissions_active", columnList = "active")
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    @Column(name = "code", nullable = false, length = 100)
    private String code; // VD: USER_READ, USER_WRITE, CENTER_MANAGE

    @Column(name = "name", nullable = false, length = 255)
    private String name; // Tên hiển thị

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết

    @Column(name = "category", length = 100)
    private String category; // Nhóm quyền: USER, CENTER, ROLE, PERMISSION, SYSTEM

    @Column(name = "active", nullable = false)
    private Boolean active = true; // Quyền có đang hoạt động

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Permission() {
    }

    public Permission(String code, String name, String description, String category) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}