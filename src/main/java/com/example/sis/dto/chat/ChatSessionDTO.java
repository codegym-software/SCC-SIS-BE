package com.example.sis.dto.chat;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ChatSessionDTO {
    private Integer sessionId;
    private String title;
    private Map<String, Object> context;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer messageCount;
}
