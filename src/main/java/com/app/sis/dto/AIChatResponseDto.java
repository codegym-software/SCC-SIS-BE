package com.app.sis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResponseDto {
    private String message;
    private String responseType; // "rule-based" or "openai"
    private boolean success = true;
}
