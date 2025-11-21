package com.example.sis.dtos.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BroadcastNotificationRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String message;
    
    // Nếu null hoặc empty thì gửi cho tất cả user
    private List<Integer> recipientIds;
    
    private String severity = "INFO"; // INFO, WARNING, ERROR
    
    // Getters and Setters
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
    
    public List<Integer> getRecipientIds() {
        return recipientIds;
    }
    
    public void setRecipientIds(List<Integer> recipientIds) {
        this.recipientIds = recipientIds;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
