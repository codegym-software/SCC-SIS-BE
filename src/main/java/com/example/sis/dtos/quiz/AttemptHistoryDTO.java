package com.example.sis.dtos.quiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AttemptHistoryDTO {
    
    private Integer quizId;
    private String quizTitle;
    private Integer maxAttempts;
    private List<AttemptSummaryDTO> attempts;
    private BigDecimal bestScore;
    private Boolean canRetake;
    private Integer attemptsRemaining;
    
    public AttemptHistoryDTO() {}

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public List<AttemptSummaryDTO> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<AttemptSummaryDTO> attempts) {
        this.attempts = attempts;
    }

    public BigDecimal getBestScore() {
        return bestScore;
    }

    public void setBestScore(BigDecimal bestScore) {
        this.bestScore = bestScore;
    }

    public Boolean getCanRetake() {
        return canRetake;
    }

    public void setCanRetake(Boolean canRetake) {
        this.canRetake = canRetake;
    }

    public Integer getAttemptsRemaining() {
        return attemptsRemaining;
    }

    public void setAttemptsRemaining(Integer attemptsRemaining) {
        this.attemptsRemaining = attemptsRemaining;
    }
}
