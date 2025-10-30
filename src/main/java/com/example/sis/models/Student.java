package com.example.sis.models;

import com.example.sis.enums.GenderType;
import com.example.sis.enums.OverallStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_students_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_students_email", columnList = "email"),
                @Index(name = "idx_students_full_name", columnList = "full_name"),
                @Index(name = "idx_students_overall_status", columnList = "overall_status")
        })
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Integer studentId;

    @NotBlank
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotBlank
    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    /** DB: ENUM('MALE','FEMALE','OTHER') */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16)
    private GenderType gender;

    @Column(name = "national_id_no", length = 64)
    private String nationalIdNo;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "province", length = 128)
    private String province;

    @Column(name = "district", length = 128)
    private String district;

    @Column(name = "ward", length = 128)
    private String ward;

    /** DB: ENUM UPPERCASE */
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", length = 50, nullable = false)
    private OverallStatus overallStatus = OverallStatus.PENDING;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Liên kết với tài khoản User để đăng nhập hệ thống
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_students_user"))
    private User user;

    // Audit (FK → users.user_id) — LAZY để list nhanh
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_students_created_by"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_students_updated_by"))
    private User updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void _prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void _preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Getters/Setters =====
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public GenderType getGender() { return gender; }
    public void setGender(GenderType gender) { this.gender = gender; }
    public String getNationalIdNo() { return nationalIdNo; }
    public void setNationalIdNo(String nationalIdNo) { this.nationalIdNo = nationalIdNo; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public OverallStatus getOverallStatus() { return overallStatus; }
    public void setOverallStatus(OverallStatus overallStatus) { this.overallStatus = overallStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
