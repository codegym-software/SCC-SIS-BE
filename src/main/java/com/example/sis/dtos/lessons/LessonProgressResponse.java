package com.example.sis.dtos.lessons;

import com.example.sis.enums.ProgressStatus;
import java.time.LocalDateTime;

public class LessonProgressResponse {
    private Integer progressId;
    private Integer studentId;
    private Integer lessonId;
    private ProgressStatus status;
    private Integer progressPercentage;
    private Integer timeSpentSeconds;
    private Integer lastWatchedPosition;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    
    // Getters and Setters
    public Integer getProgressId() {
        return progressId;
    }
    
    public void setProgressId(Integer progressId) {
        this.progressId = progressId;
    }
    
    public Integer getStudentId() {
        return studentId;
    }
    
    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }
    
    public Integer getLessonId() {
        return lessonId;
    }
    
    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }
    
    public ProgressStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProgressStatus status) {
        this.status = status;
    }
    
    public Integer getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }
    
    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
    
    public Integer getLastWatchedPosition() {
        return lastWatchedPosition;
    }
    
    public void setLastWatchedPosition(Integer lastWatchedPosition) {
        this.lastWatchedPosition = lastWatchedPosition;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
}
