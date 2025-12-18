package com.example.sis.dto.chat;

import com.example.sis.entity.MessageSource;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageResponse {
    private Integer sessionId;
    private String message;
    private List<MessageSource> sources;
    private Integer completionMs;
    private LocalDateTime timestamp;
}
