package com.example.sis.dtos.role;

import java.time.LocalDateTime;
import java.util.List;

public class RoleListItem {
    private Integer roleId;
    private String code;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
    private Long userCount;
    private Integer permissionCount;
    private List<String> permissionNamesPreview;

    public RoleListItem() {}

    public RoleListItem(Integer roleId, String code, String name, boolean active,
                       LocalDateTime createdAt, Long userCount, Integer permissionCount,
                       List<String> permissionNamesPreview) {
        this.roleId = roleId;
        this.code = code;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
        this.userCount = userCount;
        this.permissionCount = permissionCount;
        this.permissionNamesPreview = permissionNamesPreview;
    }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }

    public Integer getPermissionCount() { return permissionCount; }
    public void setPermissionCount(Integer permissionCount) { this.permissionCount = permissionCount; }

    public List<String> getPermissionNamesPreview() { return permissionNamesPreview; }
    public void setPermissionNamesPreview(List<String> permissionNamesPreview) { this.permissionNamesPreview = permissionNamesPreview; }
}