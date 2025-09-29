package com.example.sis.dtos.role;

import com.example.sis.constants.RoleScope; // <— dùng enum ở constants

public class RoleResponse {
    private Integer roleId;
    private String code;
    private String name;
    private RoleScope scope;
    private boolean active;

    public RoleResponse() {}

    public RoleResponse(Integer roleId, String code, String name, RoleScope scope, boolean active) {
        this.roleId = roleId;
        this.code = code;
        this.name = name;
        this.scope = scope;
        this.active = active;
    }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoleScope getScope() { return scope; }
    public void setScope(RoleScope scope) { this.scope = scope; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
