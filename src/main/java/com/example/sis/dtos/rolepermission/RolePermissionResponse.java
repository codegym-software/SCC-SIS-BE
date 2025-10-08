package com.example.sis.dtos.rolepermission;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.role.RoleResponse;
import java.time.LocalDateTime;

public class RolePermissionResponse {

    private Integer rolePermissionId;
    private RoleResponse role;
    private PermissionResponse permission;
    private LocalDateTime grantedAt;
    private String grantedBy;

    // Constructors
    public RolePermissionResponse() {
    }

    public RolePermissionResponse(Integer rolePermissionId, RoleResponse role,
            PermissionResponse permission, LocalDateTime grantedAt,
            String grantedBy) {
        this.rolePermissionId = rolePermissionId;
        this.role = role;
        this.permission = permission;
        this.grantedAt = grantedAt;
        this.grantedBy = grantedBy;
    }

    // Getters and Setters
    public Integer getRolePermissionId() {
        return rolePermissionId;
    }

    public void setRolePermissionId(Integer rolePermissionId) {
        this.rolePermissionId = rolePermissionId;
    }

    public RoleResponse getRole() {
        return role;
    }

    public void setRole(RoleResponse role) {
        this.role = role;
    }

    public PermissionResponse getPermission() {
        return permission;
    }

    public void setPermission(PermissionResponse permission) {
        this.permission = permission;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }
}