package com.example.sis.dto.chat;

import com.example.sis.entity.MessageSource;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatMessageDTO {
    private Integer messageId;
    private String role;
    private String content;
    private List<MessageSource> sources;
    private Integer completionMs;
    private LocalDateTime createdAt;
}
