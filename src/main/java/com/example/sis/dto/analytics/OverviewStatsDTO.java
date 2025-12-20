package com.example.sis.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStatsDTO {
    private Long totalQuestions;
    private Long totalUsers;
    private Integer avgResponseTime;
    private Double questionsTrend;
    private Double usersTrend;
    private Double responseTimeTrend;
}
