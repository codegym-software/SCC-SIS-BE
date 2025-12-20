package com.example.sis.dto.analytics;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@Builder
public class ChatAnalyticsDTO {
    private Long totalSessions;
    private Long totalMessages;
    private Long totalUserMessages;
    private Long totalAssistantMessages;
    private Integer avgCompletionMs;
    private Long totalTokensUsed;
    private BigDecimal totalCostUsd;
    private BigDecimal avgCostPerMessage;
}
