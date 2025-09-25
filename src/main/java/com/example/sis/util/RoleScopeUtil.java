// src/main/java/com/example/sis/util/RoleScopeUtil.java
package com.example.sis.util;

import com.example.sis.constants.RoleCodes;

public final class RoleScopeUtil {
    private RoleScopeUtil() {}

    // Vai trò "độc quyền" & global-only: chọn 1 mình nó, centerId = null
    public static boolean isExclusiveGlobal(String code) {
        return RoleCodes.SUPER_ADMIN.equals(code) || RoleCodes.TRAINING_MANAGER.equals(code);
    }

    // Vai trò phải theo center
    public static boolean isCenterScoped(String code) {
        return RoleCodes.CENTER_MANAGER.equals(code)
                || RoleCodes.ACADEMIC_STAFF.equals(code)
                || RoleCodes.LECTURER.equals(code);
    }
}
