package com.example.sis.dtos.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UpdateRoleRequest {

    @Size(max = 64, message = "Mã role không được vượt quá 64 ký tự")
    private String code;

    @Size(max = 128, message = "Tên role không được vượt quá 128 ký tự")
    private String name;

    private Boolean active;

    private List<Integer> permissionIds;

    // Constructors
    public UpdateRoleRequest() {
    }

    public UpdateRoleRequest(String code, String name, Boolean active, List<Integer> permissionIds) {
        this.code = code;
        this.name = name;
        this.active = active;
        this.permissionIds = permissionIds;
    }

    // Getters and Setters
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

    public List<Integer> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Integer> permissionIds) {
        this.permissionIds = permissionIds;
    }
}