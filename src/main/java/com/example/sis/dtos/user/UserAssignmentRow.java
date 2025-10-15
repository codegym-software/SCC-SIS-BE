package com.example.sis.dtos.user;

public class UserAssignmentRow {

    // --- User fields ---
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private boolean active;
    private String specialty; // chuyên môn

    // --- Assignment fields (có thể null nếu user chưa có role) ---
    private Integer userRoleId; // để làm assignmentId cho FE hủy gán vai trò
    private Integer roleId;
    private String roleCode;
    private String roleName;
    private java.time.LocalDateTime assignedAt; // từ user_roles.assignedAt

    private Integer centerId;   // null nếu GLOBAL
    private String centerName;  // null nếu GLOBAL

    // JPQL constructor projection
    public UserAssignmentRow(Integer userId,
                             String fullName,
                             String email,
                             String phone,
                             boolean active,
                             String specialty,
                             Integer userRoleId,
                             Integer roleId,
                             String roleCode,
                             String roleName,
                             java.time.LocalDateTime assignedAt,
                             Integer centerId,
                             String centerName) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.specialty = specialty;
        this.userRoleId = userRoleId;
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.assignedAt = assignedAt;
        this.centerId = centerId;
        this.centerName = centerName;
    }

    public UserAssignmentRow() {
    }

    // --- getters/setters ---

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public java.time.LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(java.time.LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getCenterId() {
        return centerId;
    }

    public void setCenterId(Integer centerId) {
        this.centerId = centerId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }
}
