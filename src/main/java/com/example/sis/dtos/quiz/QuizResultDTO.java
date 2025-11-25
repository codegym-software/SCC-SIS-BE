package com.example.sis.dtos.quiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class QuizResultDTO {
    
    private Integer attemptId;
    private BigDecimal score;
    private Integer totalPoints;
    private Integer percentage;
    private String status;
    private LocalDateTime completedAt;
    private Integer timeSpentSeconds;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Boolean isPassed;
    private Boolean canRetake;
    private Integer attemptsRemaining;
    private List<QuestionResultDTO> results;
    
    public QuizResultDTO() {}

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public Integer getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Boolean getIsPassed() {
        return isPassed;
    }

    public void setIsPassed(Boolean isPassed) {
        this.isPassed = isPassed;
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

    public List<QuestionResultDTO> getResults() {
        return results;
    }

    public void setResults(List<QuestionResultDTO> results) {
        this.results = results;
    }
}
