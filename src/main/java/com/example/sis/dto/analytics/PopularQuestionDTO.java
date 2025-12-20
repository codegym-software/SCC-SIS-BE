package com.example.sis.dto.analytics;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PopularQuestionDTO {
    private String question;
    private Long count;
    private Integer avgCompletionMs;
    private Double satisfactionRate; // 0-100 percentage
}
