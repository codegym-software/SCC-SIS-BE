// src/main/java/com/example/sis/dto/user/UserResponse.java
package com.example.sis.dtos.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String keycloakUserId;
    private Integer defaultRoleId;
    private Integer defaultCenterId;
    private LocalDate dob;
    private String gender;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // getters/setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(String keycloakUserId) { this.keycloakUserId = keycloakUserId; }

    public Integer getDefaultRoleId() { return defaultRoleId; }
    public void setDefaultRoleId(Integer defaultRoleId) { this.defaultRoleId = defaultRoleId; }

    public Integer getDefaultCenterId() { return defaultCenterId; }
    public void setDefaultCenterId(Integer defaultCenterId) { this.defaultCenterId = defaultCenterId; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

}
