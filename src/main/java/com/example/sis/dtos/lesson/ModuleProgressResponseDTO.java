package com.example.sis.dtos.lesson;

public class ModuleProgressResponseDTO {
    
    private Integer moduleId;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer progressPercentage;
    private Integer totalTimeSpentSeconds;
    
    public ModuleProgressResponseDTO() {}

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Integer getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(Integer totalLessons) {
        this.totalLessons = totalLessons;
    }

    public Integer getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(Integer completedLessons) {
        this.completedLessons = completedLessons;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Integer getTotalTimeSpentSeconds() {
        return totalTimeSpentSeconds;
    }

    public void setTotalTimeSpentSeconds(Integer totalTimeSpentSeconds) {
        this.totalTimeSpentSeconds = totalTimeSpentSeconds;
    }
}
