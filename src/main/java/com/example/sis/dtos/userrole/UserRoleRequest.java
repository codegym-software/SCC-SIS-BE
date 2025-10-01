package com.example.sis.dtos.userrole;

import jakarta.validation.constraints.NotNull;

public class UserRoleRequest {

    @NotNull(message = "User ID không được để trống")
    private Integer userId;

    @NotNull(message = "Role ID không được để trống")
    private Integer roleId;

    private Integer centerId; // Có thể null cho role global

    // Constructors
    public UserRoleRequest() {
    }

    public UserRoleRequest(Integer userId, Integer roleId, Integer centerId) {
        this.userId = userId;
        this.roleId = roleId;
        this.centerId = centerId;
    }

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getCenterId() {
        return centerId;
    }

    public void setCenterId(Integer centerId) {
        this.centerId = centerId;
    }
}