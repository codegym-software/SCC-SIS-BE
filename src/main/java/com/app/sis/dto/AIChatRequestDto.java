package com.app.sis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChatRequestDto {
    private String message;
    private Long userId;
    private String userName;
    private boolean useOpenAI = false; // Default to rule-based
}
