package com.example.sis.dtos.enrollment;

import com.example.sis.enums.EnrollmentStatus;
import java.time.LocalDate;

/**
 * DTO dùng khi cập nhật trạng thái / ngày kết thúc ghi danh.
 */
public class UpdateEnrollmentRequest {

    private EnrollmentStatus status;
    private LocalDate leftAt;
    private String note;

    public EnrollmentStatus getStatus() {
        return status;
    }
    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public LocalDate getLeftAt() {
        return leftAt;
    }
    public void setLeftAt(LocalDate leftAt) {
        this.leftAt = leftAt;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }
}
