package com.example.sis.dtos.rolepermission;

import jakarta.validation.constraints.NotNull;

public class RolePermissionRequest {

    @NotNull(message = "Role ID không được để trống")
    private Integer roleId;

    @NotNull(message = "Permission ID không được để trống")
    private Integer permissionId;

    // Constructors
    public RolePermissionRequest() {
    }

    public RolePermissionRequest(Integer roleId, Integer permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    // Getters and Setters
    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }
}