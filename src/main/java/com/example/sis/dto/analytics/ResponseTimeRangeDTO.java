package com.example.sis.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTimeRangeDTO {
    private String range;
    private Long count;
    private Double percentage;
}
