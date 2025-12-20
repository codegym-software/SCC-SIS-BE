package com.example.sis.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSatisfactionDTO {
    private Double avgRating;
    private List<RatingDistributionDTO> ratingDistribution;
    private Long feedbackCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistributionDTO {
        private Integer rating;
        private Long count;
    }
}
