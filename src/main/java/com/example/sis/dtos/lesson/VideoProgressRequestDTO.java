package com.example.sis.dtos.lesson;

public class VideoProgressRequestDTO {
    
    private Integer progressPercentage;
    private Integer lastWatchedPosition;
    private Integer timeSpentSeconds;
    
    public VideoProgressRequestDTO() {}

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Integer getLastWatchedPosition() {
        return lastWatchedPosition;
    }

    public void setLastWatchedPosition(Integer lastWatchedPosition) {
        this.lastWatchedPosition = lastWatchedPosition;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
}
