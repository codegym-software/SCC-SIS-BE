package com.example.sis.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class NotificationDTO {
    
    private Long id;
    
    @NotNull(message = "userId không được để trống")
    private Integer userId;
    
    @NotBlank(message = "type không được để trống")
    private String type;
    
    @NotBlank(message = "title không được để trống")
    private String title;
    
    @NotBlank(message = "message không được để trống")
    private String message;
    
    private String relatedType;
    private Long relatedId;
    private String severity;
    private Boolean isRead;
    private LocalDateTime createdAt;
    
    // Constructors
    public NotificationDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRelatedType() {
        return relatedType;
    }
    
    public void setRelatedType(String relatedType) {
        this.relatedType = relatedType;
    }
    
    public Long getRelatedId() {
        return relatedId;
    }
    
    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
