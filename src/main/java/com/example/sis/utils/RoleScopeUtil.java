package com.example.sis.utils;

import com.example.sis.enums.RoleScope;

public final class RoleScopeUtil {
    private RoleScopeUtil() {}

    // role độc quyền toàn hệ thống
    public static boolean isExclusiveGlobal(String code) {
        return "SUPER_ADMIN".equals(code) || "TRAINING_MANAGER".equals(code);
    }

    // role thuộc center
    public static boolean isCenterScoped(String code) {
        return "CENTER_MANAGER".equals(code)
                || "ACADEMIC_STAFF".equals(code)
                || "LECTURER".equals(code);
    }

    // nếu sau này muốn dùng enum RoleScope
    public static RoleScope resolveScope(String code) {
        if (isExclusiveGlobal(code)) return RoleScope.GLOBAL;
        if (isCenterScoped(code)) return RoleScope.CENTER;
        throw new IllegalArgumentException("Unknown role code: " + code);
    }
}
