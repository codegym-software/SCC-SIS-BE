package com.example.sis.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {
    private Integer sessionId; // null for new session
    
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 5000, message = "Message must be between 1 and 5000 characters")
    private String message;
    
    // Optional: Frontend can pass explicit context (priority over auto-detection)
    private Integer classId;
    private Integer moduleId;
    private Integer lessonId;
}
