package com.example.sis.enums;

public enum StudyDay {
    MONDAY("Thứ 2"),
    TUESDAY("Thứ 3"),
    WEDNESDAY("Thứ 4"),
    THURSDAY("Thứ 5"),
    FRIDAY("Thứ 6"),
    SATURDAY("Thứ 7"),
    SUNDAY("Chủ nhật");

    private final String displayName;

    StudyDay(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
