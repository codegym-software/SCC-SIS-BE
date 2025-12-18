package com.example.sis.dto.chat;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SafeChatContext {
    // Public user info (NO tokens, NO sensitive data)
    private String userName;
    private String userRole;
    
    // Academic context
    private Integer classId;
    private String className;
    private Integer moduleId;
    private String moduleName;
    private Integer lessonId;
    private String lessonTitle;
    
    // Preferences
    private String language; // vi, en
}
