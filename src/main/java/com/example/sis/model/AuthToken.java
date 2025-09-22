package com.example.sis.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_tokens_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "idx_auth_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_tokens_expires_at", columnList = "expires_at")
        })
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    private Integer authTokenId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_auth_tokens_user"))
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash; // chỉ lưu hash

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    public AuthToken() {}

    // Getters & Setters
    public Integer getAuthTokenId() { return authTokenId; }
    public void setAuthTokenId(Integer authTokenId) { this.authTokenId = authTokenId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
