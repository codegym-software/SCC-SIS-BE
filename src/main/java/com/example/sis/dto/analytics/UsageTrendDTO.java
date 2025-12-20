package com.example.sis.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageTrendDTO {
    private String date;
    private Long questionCount;
    private Long userCount;
    private Integer avgResponseTime;
}
