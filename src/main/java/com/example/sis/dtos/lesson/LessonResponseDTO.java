package com.example.sis.dtos.lesson;

import com.example.sis.enums.ContentType;
import com.example.sis.enums.LessonType;
import com.example.sis.enums.ProgressStatus;

import java.time.LocalDateTime;

public class LessonResponseDTO {
    
    private Integer lessonId;
    private Integer moduleId;
    private String lessonTitle;
    private LessonType lessonType;
    private Integer lessonOrder;
    private String contentUrl;
    private ContentType contentType;
    private Integer durationMinutes;
    private String description;
    private Boolean isMandatory;
    private Integer passingScore;
    private Integer moduleSemester; // Semester of the module this lesson belongs to
    private String moduleName; // Name of the module this lesson belongs to
    
    // Progress information
    private ProgressStatus status;
    private Integer progressPercentage;
    private Integer timeSpentSeconds;
    private Integer lastWatchedPosition;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    
    public LessonResponseDTO() {}

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public void setLessonType(LessonType lessonType) {
        this.lessonType = lessonType;
    }

    public Integer getLessonOrder() {
        return lessonOrder;
    }

    public void setLessonOrder(Integer lessonOrder) {
        this.lessonOrder = lessonOrder;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public Integer getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(Integer passingScore) {
        this.passingScore = passingScore;
    }

    public Integer getModuleSemester() {
        return moduleSemester;
    }

    public void setModuleSemester(Integer moduleSemester) {
        this.moduleSemester = moduleSemester;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
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
