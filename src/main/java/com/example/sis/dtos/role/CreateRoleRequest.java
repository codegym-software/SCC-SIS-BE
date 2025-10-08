package com.example.sis.dtos.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateRoleRequest {

    @NotBlank(message = "Mã role không được để trống")
    @Size(max = 64, message = "Mã role không được vượt quá 64 ký tự")
    private String code;

    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 128, message = "Tên role không được vượt quá 128 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    private String scope; // GLOBAL, CENTER

    private Boolean active = true;

    // Constructors
    public CreateRoleRequest() {
    }

    public CreateRoleRequest(String code, String name, String description, String scope, Boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.active = active;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}