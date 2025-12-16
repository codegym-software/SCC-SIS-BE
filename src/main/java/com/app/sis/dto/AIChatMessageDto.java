package com.app.sis.dto;

import java.time.LocalDateTime;

public class AIChatMessageDto {
    private String role;        // "user" hoặc "assistant"
    private String content;     // Nội dung tin nhắn
    private LocalDateTime timestamp;

    public AIChatMessageDto() {
    }

    public AIChatMessageDto(String role, String content, LocalDateTime timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
