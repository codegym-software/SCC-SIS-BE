package com.example.sis.models;

import com.example.sis.enums.ProgressStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress")
public class LessonProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;
    
    @Column(name = "student_id", nullable = false)
    private Integer studentId;
    
    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProgressStatus status = ProgressStatus.NOT_STARTED;
    
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;
    
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;
    
    @Column(name = "last_watched_position")
    private Integer lastWatchedPosition = 0;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "first_accessed_at")
    private LocalDateTime firstAccessedAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    // Getters and Setters
    public Integer getProgressId() { return progressId; }
    public void setProgressId(Integer progressId) { this.progressId = progressId; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public ProgressStatus getStatus() { return status; }
    public void setStatus(ProgressStatus status) { this.status = status; }

    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }

    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public Integer getLastWatchedPosition() { return lastWatchedPosition; }
    public void setLastWatchedPosition(Integer lastWatchedPosition) { this.lastWatchedPosition = lastWatchedPosition; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getFirstAccessedAt() { return firstAccessedAt; }
    public void setFirstAccessedAt(LocalDateTime firstAccessedAt) { this.firstAccessedAt = firstAccessedAt; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
}
