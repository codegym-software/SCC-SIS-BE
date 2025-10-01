package com.example.sis.dtos.userrole;

import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.user.UserResponse;
import java.time.LocalDateTime;

public class UserRoleResponse {

    private Integer userRoleId;
    private UserResponse user;
    private RoleResponse role;
    private CenterResponse center; // Có thể null cho role global
    private LocalDateTime assignedAt;
    private String assignedBy;
    private LocalDateTime revokedAt;
    private String revokedBy;

    // Constructors
    public UserRoleResponse() {
    }

    public UserRoleResponse(Integer userRoleId, UserResponse user, RoleResponse role,
            CenterResponse center, LocalDateTime assignedAt, String assignedBy,
            LocalDateTime revokedAt, String revokedBy) {
        this.userRoleId = userRoleId;
        this.user = user;
        this.role = role;
        this.center = center;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
    }

    // Getters and Setters
    public Integer getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public RoleResponse getRole() {
        return role;
    }

    public void setRole(RoleResponse role) {
        this.role = role;
    }

    public CenterResponse getCenter() {
        return center;
    }

    public void setCenter(CenterResponse center) {
        this.center = center;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }
}