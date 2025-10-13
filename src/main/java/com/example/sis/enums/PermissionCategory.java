package com.example.sis.enums;

/**
 * Enum định nghĩa các category của permission với thứ tự hiển thị
 */
public enum PermissionCategory {
    CENTER("Trung tâm", 10),
    USER("Người dùng", 20),
    ROLE("Vai trò", 30),
    PERMISSION("Quyền hạn", 40),
    SYSTEM("Hệ thống", 50),
    CLASS("Lớp học", 60);

    private final String label;
    private final int order;

    PermissionCategory(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }

    /**
     * Tìm PermissionCategory từ code (tên enum)
     */
    public static PermissionCategory fromCode(String code) {
        if (code == null) return null;
        try {
            return PermissionCategory.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Tìm PermissionCategory từ category string trong database
     */
    public static PermissionCategory fromCategory(String category) {
        if (category == null) return null;
        for (PermissionCategory pc : values()) {
            if (pc.name().equals(category.toUpperCase())) {
                return pc;
            }
        }
        return null;
    }
}