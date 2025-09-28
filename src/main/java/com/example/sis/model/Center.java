package com.example.sis.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "centers")
public class Center {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "center_id")
        private Integer centerId;

        @NotBlank(message = "Tên trung tâm không được để trống")
        @Size(max = 255, message = "Tên trung tâm không được vượt quá 255 ký tự")
        @Column(name = "name", nullable = false)
        private String name;

        @NotBlank(message = "Mã trung tâm không được để trống")
        @Size(max = 50, message = "Mã trung tâm không được vượt quá 50 ký tự")
        @Column(name = "code", nullable = false, unique = true)
        private String code;

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
        @Column(name = "email", nullable = false)
        private String email;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        @Column(name = "phone", nullable = false)
        private String phone;

        @Column(name = "established_date")
        private LocalDate establishedDate;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;

        @NotBlank(message = "Địa chỉ không được để trống")
        @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
        @Column(name = "address_line", nullable = false)
        private String addressLine;

        @NotBlank(message = "Tỉnh/Thành phố không được để trống")
        @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
        @Column(name = "province", nullable = false)
        private String province;

        @NotBlank(message = "Quận/Huyện không được để trống")
        @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
        @Column(name = "district", nullable = false)
        private String district;

        @NotBlank(message = "Phường/Xã không được để trống")
        @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
        @Column(name = "ward", nullable = false)
        private String ward;

        @Column(name = "deleted_at")
        private LocalDateTime deletedAt;

        @Column(name = "created_by")
        private Integer createdBy;

        @Column(name = "updated_by")
        private Integer updatedBy;

        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
                createdAt = LocalDateTime.now();
                updatedAt = LocalDateTime.now();
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now();
        }

        public Center() {
        }

        public Integer getCenterId() {
                return centerId;
        }

        public void setCenterId(Integer centerId) {
                this.centerId = centerId;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getCode() {
                return code;
        }

        public void setCode(String code) {
                this.code = code;
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

        public LocalDate getEstablishedDate() {
                return establishedDate;
        }

        public void setEstablishedDate(LocalDate establishedDate) {
                this.establishedDate = establishedDate;
        }

        public String getDescription() {
                return description;
        }

        public void setDescription(String description) {
                this.description = description;
        }

        public String getAddressLine() {
                return addressLine;
        }

        public void setAddressLine(String addressLine) {
                this.addressLine = addressLine;
        }

        public String getProvince() {
                return province;
        }

        public void setProvince(String province) {
                this.province = province;
        }

        public String getDistrict() {
                return district;
        }

        public void setDistrict(String district) {
                this.district = district;
        }

        public String getWard() {
                return ward;
        }

        public void setWard(String ward) {
                this.ward = ward;
        }

        public LocalDateTime getDeletedAt() {
                return deletedAt;
        }

        public void setDeletedAt(LocalDateTime deletedAt) {
                this.deletedAt = deletedAt;
        }

        public Integer getCreatedBy() {
                return createdBy;
        }

        public void setCreatedBy(Integer createdBy) {
                this.createdBy = createdBy;
        }

        public Integer getUpdatedBy() {
                return updatedBy;
        }

        public void setUpdatedBy(Integer updatedBy) {
                this.updatedBy = updatedBy;
        }

        public LocalDateTime getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
                return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
                this.updatedAt = updatedAt;
        }
}
