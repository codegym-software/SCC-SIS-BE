package com.example.sis.dtos.quiz;

import java.time.LocalDateTime;
import java.util.List;

public class StartAttemptResponseDTO {
    
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private Integer timeLimitMinutes;
    private Integer totalQuestions;
    private Integer totalPoints;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private List<QuestionDetailDTO> questions;
    
    public StartAttemptResponseDTO() {}

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

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

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<QuestionDetailDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDetailDTO> questions) {
        this.questions = questions;
    }
}
