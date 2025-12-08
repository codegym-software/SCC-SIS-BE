package com.example.sis.dtos.lessons;

public class LessonProgressUpdateRequest {
    private Integer lessonId;
    private Integer currentPosition; // seconds
    private Integer duration; // seconds
    private Integer timeSpent; // seconds spent in this session
    
    // Getters and Setters
    public Integer getLessonId() {
        return lessonId;
    }
    
    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }
    
    public Integer getCurrentPosition() {
        return currentPosition;
    }
    
    public void setCurrentPosition(Integer currentPosition) {
        this.currentPosition = currentPosition;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Integer getTimeSpent() {
        return timeSpent;
    }
    
    public void setTimeSpent(Integer timeSpent) {
        this.timeSpent = timeSpent;
    }
}
