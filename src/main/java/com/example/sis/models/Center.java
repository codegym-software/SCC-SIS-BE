package com.example.sis.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "centers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_centers_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_centers_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_centers_code", columnList = "code"),
                @Index(name = "idx_centers_name", columnList = "name")
        }
)
public class Center {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "center_id")
        private Integer centerId;

        @NotBlank(message = "Mã trung tâm không được để trống")
        @Size(max = 32, message = "Mã trung tâm không được vượt quá 32 ký tự")
        @Column(name = "code", nullable = false, length = 32)
        @Comment("Mã ngắn (VD: HN, HCM)")
        private String code;

        @NotBlank(message = "Tên trung tâm không được để trống")
        @Size(max = 255, message = "Tên trung tâm không được vượt quá 255 ký tự")
        @Column(name = "name", nullable = false, length = 255)
        private String name;

        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
        @Column(name = "email", length = 255)
        private String email;

        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        @Column(name = "phone", length = 20)
        private String phone;

        @Column(name = "established_date")
        private LocalDate establishedDate;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;

        @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
        @Column(name = "address_line", length = 500)
        private String addressLine;

        @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
        @Column(name = "province", length = 100)
        private String province;

        @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
        @Column(name = "district", length = 100)
        private String district;

        @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
        @Column(name = "ward", length = 100)
        private String ward;

        // Soft delete
        @Column(name = "deleted_at")
        private LocalDateTime deletedAt;

        // Audit (ai tạo/cập nhật)
        @Column(name = "created_by")
        private Integer createdBy;

        @Column(name = "updated_by")
        private Integer updatedBy;

        // Audit timestamps
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        public Center() {}

        @PrePersist
        protected void onCreate() {
                LocalDateTime now = LocalDateTime.now();
                this.createdAt = now;
                this.updatedAt = now;
        }

        @PreUpdate
        protected void onUpdate() {
                this.updatedAt = LocalDateTime.now();
        }

        // Getters & Setters
        public Integer getCenterId() { return centerId; }
        public void setCenterId(Integer centerId) { this.centerId = centerId; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public LocalDate getEstablishedDate() { return establishedDate; }
        public void setEstablishedDate(LocalDate establishedDate) { this.establishedDate = establishedDate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getAddressLine() { return addressLine; }
        public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }

        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

        public Integer getCreatedBy() { return createdBy; }
        public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

        public Integer getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
