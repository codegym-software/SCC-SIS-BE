package com.app.sis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChatMessageDto {
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;
}
