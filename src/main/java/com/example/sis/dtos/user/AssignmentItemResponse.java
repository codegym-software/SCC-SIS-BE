package com.example.sis.dtos.user;

import com.example.sis.enums.RoleScope;

public class AssignmentItemResponse {

     private Integer assignmentId; // alias của userRoleId - để FE dùng hủy gán vai trò
     private Integer roleId;
     private String roleCode;
     private String roleName;

     private RoleScope scope;   // GLOBAL | CENTER

     private Integer centerId;  // null nếu GLOBAL
     private String centerName; // null nếu GLOBAL
     private java.time.LocalDateTime assignedAt; // ISO-8601 từ user_roles.assignedAt

     public AssignmentItemResponse() {
     }

    public AssignmentItemResponse(Integer assignmentId,
                                  Integer roleId,
                                  String roleCode,
                                  String roleName,
                                  RoleScope scope,
                                  Integer centerId,
                                  String centerName,
                                  java.time.LocalDateTime assignedAt) {
        this.assignmentId = assignmentId;
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.scope = scope;
        this.centerId = centerId;
        this.centerName = centerName;
        this.assignedAt = assignedAt;
    }

    // --- getters/setters ---

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public RoleScope getScope() {
        return scope;
    }

    public void setScope(RoleScope scope) {
        this.scope = scope;
    }

    public Integer getCenterId() {
        return centerId;
    }

    public void setCenterId(Integer centerId) {
        this.centerId = centerId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public java.time.LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(java.time.LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
