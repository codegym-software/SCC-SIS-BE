package com.example.sis.enums;

public enum StudyTime {
    MORNING("8:00-11:00"),
    AFTERNOON("14:00-17:00"),
    EVENING("18:00-21:00");

    private final String timeRange;

    StudyTime(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getTimeRange() {
        return timeRange;
    }
}
