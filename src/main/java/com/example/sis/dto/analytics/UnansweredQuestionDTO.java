package com.example.sis.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnansweredQuestionDTO {
    private Long questionId;
    private String question;
    private Double avgSimilarity;
    private Integer askedCount;
    private LocalDateTime lastAsked;
}
