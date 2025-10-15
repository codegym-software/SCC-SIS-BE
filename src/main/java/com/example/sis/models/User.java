package com.example.sis.models;

import com.example.sis.enums.GenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_keycloak_id", columnNames = "keycloak_user_id")
        },
        indexes = {
                @Index(name = "idx_users_full_name", columnList = "full_name"),
                @Index(name = "idx_users_phone", columnList = "phone"),
                @Index(name = "idx_users_keycloak_id", columnList = "keycloak_user_id"),
                @Index(name = "idx_users_default_role", columnList = "default_role_id"),
                @Index(name = "idx_users_default_center", columnList = "default_center_id")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    // (*) bắt buộc nhập
    @NotBlank
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    // Liên kết với Keycloak
    @NotBlank
    @Column(name = "keycloak_user_id", nullable = false, length = 64)
    private String keycloakUserId;

    // Default role và center cho auto-assignment
    @Column(name = "default_role_id")
    private Integer defaultRoleId;

    @Column(name = "default_center_id")
    private Integer defaultCenterId;

    // Thông tin bổ sung
    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16)
    private GenderType gender;

    @Column(name = "national_id_no", length = 64)
    private String nationalIdNo;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "specialty", length = 255)
    private String specialty;

    @Column(name = "experience", columnDefinition = "TEXT")
    private String experience;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "province", length = 128)
    private String province;

    @Column(name = "district", length = 128)
    private String district;

    @Column(name = "ward", length = 128)
    private String ward;

    @Column(name = "education_level", length = 128)
    private String educationLevel;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Trạng thái
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Audit/soft delete
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public User() {}

    // Getters & Setters
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

    public GenderType getGender() { return gender; }
    public void setGender(GenderType gender) { this.gender = gender; }

    public String getNationalIdNo() { return nationalIdNo; }
    public void setNationalIdNo(String nationalIdNo) { this.nationalIdNo = nationalIdNo; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
