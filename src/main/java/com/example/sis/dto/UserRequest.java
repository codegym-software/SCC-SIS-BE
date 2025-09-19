package com.example.sis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // Khi tạo user: bắt buộc, khi update: có thể null/blank
    @Size(min = 8, max = 128)
    private String password;

    // Khi update: có thể gửi để bật/tắt user
    private Boolean isActive;

    public UserRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
