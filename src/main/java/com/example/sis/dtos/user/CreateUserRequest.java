// src/main/java/com/example/sis/dto/user/CreateUserRequest.java
package com.example.sis.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class CreateUserRequest {

    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String phone;

    /** Có thể null: nếu null, BE sẽ tự tạo/tìm user trên Keycloak theo email */
    private String keycloakUserId;

    // Optional
    private LocalDate dob;
    private String gender; // "male" | "female"
    private String nationalIdNo;
    private LocalDate startDate;
    private String specialty;
    private String experience;
    private String addressLine;
    private String province;
    private String district;
    private String ward;
    private String educationLevel;
    private String note;

    /** Danh sách role cần gán ngay */
    private List<RoleAssignment> roles;

    /** Default role cho auto-assignment (optional) */
    private Integer defaultRoleId;

    /** Default center cho auto-assignment (optional, chỉ cần thiết nếu role scope = CENTER) */
    private Integer defaultCenterId;

    // getters/setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(String keycloakUserId) { this.keycloakUserId = keycloakUserId; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
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
    public List<RoleAssignment> getRoles() { return roles; }
    public void setRoles(List<RoleAssignment> roles) { this.roles = roles; }

    public Integer getDefaultRoleId() { return defaultRoleId; }
    public void setDefaultRoleId(Integer defaultRoleId) { this.defaultRoleId = defaultRoleId; }

    public Integer getDefaultCenterId() { return defaultCenterId; }
    public void setDefaultCenterId(Integer defaultCenterId) { this.defaultCenterId = defaultCenterId; }

    public static class RoleAssignment {
        @NotNull
        private Integer roleId;
        private Integer centerId; // null = global

        public Integer getRoleId() { return roleId; }
        public void setRoleId(Integer roleId) { this.roleId = roleId; }
        public Integer getCenterId() { return centerId; }
        public void setCenterId(Integer centerId) { this.centerId = centerId; }
    }
}
