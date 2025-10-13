package com.example.sis.dtos.permission;

/**
 * DTO cho từng permission item trong nhóm
 */
public class PermissionGroupItemResponse {
    private Integer permissionId;
    private String code;
    private String name;
    private Boolean active;
    private Boolean granted; // Chỉ có khi truyền roleId

    // Constructors
    public PermissionGroupItemResponse() {
    }

    public PermissionGroupItemResponse(Integer permissionId, String code, String name,
                                       Boolean active, Boolean granted) {
        this.permissionId = permissionId;
        this.code = code;
        this.name = name;
        this.active = active;
        this.granted = granted;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }
}