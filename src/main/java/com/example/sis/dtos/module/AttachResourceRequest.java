package com.example.sis.dtos.module;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * DTO để gắn/cập nhật tài liệu (resourceUrl) cho module
 * 
 * Sử dụng cho API:
 * - PUT /api/modules/{moduleId}/resource
 */
public class AttachResourceRequest {

    @Size(max = 1000, message = "Resource URL không được vượt quá 1000 ký tự")
    @Pattern(regexp = "^(https?://.*)?$", message = "Resource URL phải là URL hợp lệ (http/https)")
    private String resourceUrl;

    // ===== Getters & Setters =====

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }
}
