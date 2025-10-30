// com/example/sis/enums/OverallStatus.java
package com.example.sis.enums;

/** Trạng thái tổng quát của hồ sơ học viên */
public enum OverallStatus {
    PENDING,    // đang chờ (mặc định khi tạo mới)
    ACTIVE,      // đang học (khi được gán vào lớp)
    INACTIVE,    // không hoạt động
    GRADUATED,   // đã tốt nghiệp
    SUSPENDED,   // tạm dừng
    DROPPED      // nghỉ học
}
