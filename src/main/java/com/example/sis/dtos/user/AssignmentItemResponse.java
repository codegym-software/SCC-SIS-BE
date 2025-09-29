package com.example.sis.dtos.user;

import com.example.sis.constants.RoleScope;

public class AssignmentItemResponse {

    private Integer roleId;
    private String roleCode;
    private String roleName;

    private RoleScope scope;   // GLOBAL | CENTER

    private Integer centerId;  // null nếu GLOBAL
    private String centerName; // null nếu GLOBAL

    public AssignmentItemResponse() {
    }

    public AssignmentItemResponse(Integer roleId,
                                  String roleCode,
                                  String roleName,
                                  RoleScope scope,
                                  Integer centerId,
                                  String centerName) {
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.scope = scope;
        this.centerId = centerId;
        this.centerName = centerName;
    }

    // --- getters/setters ---

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
}
