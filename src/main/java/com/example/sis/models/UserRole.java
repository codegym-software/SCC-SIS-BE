package com.example.sis.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles", indexes = {
                @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
                @Index(name = "idx_user_roles_center_id", columnList = "center_id"),
                @Index(name = "idx_user_roles_user_revoked", columnList = "user_id, revoked_at"),
                @Index(name = "idx_user_roles_center_revoked", columnList = "center_id, revoked_at")
})
public class UserRole {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "user_role_id")
        private Integer userRoleId;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_user"))
        private User user;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_role"))
        private Role role;

        @ManyToOne(optional = true, fetch = FetchType.LAZY) // <-- cho phép NULL
        @JoinColumn(name = "center_id", nullable = true, foreignKey = @ForeignKey(name = "fk_user_roles_center"))
        private Center center; // null = role global

        @Column(name = "assigned_at")
        private LocalDateTime assignedAt;

        @Column(name = "assigned_by", length = 255)
        private String assignedBy; // Keycloak ID của người gán

        @Column(name = "revoked_at")
        private LocalDateTime revokedAt;

        @Column(name = "revoked_by", length = 255)
        private String revokedBy; // Keycloak ID của người thu hồi

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        public UserRole() {
        }

        public Integer getUserRoleId() {
                return userRoleId;
        }

        public void setUserRoleId(Integer userRoleId) {
                this.userRoleId = userRoleId;
        }

        public User getUser() {
                return user;
        }

        public void setUser(User user) {
                this.user = user;
        }

        public Role getRole() {
                return role;
        }

        public void setRole(Role role) {
                this.role = role;
        }

        public Center getCenter() {
                return center;
        }

        public void setCenter(Center center) {
                this.center = center;
        }

        public LocalDateTime getAssignedAt() {
                return assignedAt;
        }

        public void setAssignedAt(LocalDateTime assignedAt) {
                this.assignedAt = assignedAt;
        }

        public String getAssignedBy() {
                return assignedBy;
        }

        public void setAssignedBy(String assignedBy) {
                this.assignedBy = assignedBy;
        }

        public LocalDateTime getRevokedAt() {
                return revokedAt;
        }

        public void setRevokedAt(LocalDateTime revokedAt) {
                this.revokedAt = revokedAt;
        }

        public String getRevokedBy() {
                return revokedBy;
        }

        public void setRevokedBy(String revokedBy) {
                this.revokedBy = revokedBy;
        }

        public LocalDateTime getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
        }
}
