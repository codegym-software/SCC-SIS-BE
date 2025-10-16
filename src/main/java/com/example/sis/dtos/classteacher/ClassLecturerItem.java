package com.example.sis.dtos.classteacher;

import java.time.LocalDate;
import java.time.Instant;

/**
 * DTO cho thông tin chi tiết gán giảng viên vào lớp
 */
public record ClassLecturerItem(
    Long assignmentId,
    Long classId,
    LecturerLite lecturer,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    String note,
    Instant createdAt,
    String assignedBy,
    String revokedBy,
    boolean canEdit,
    boolean canRemove
) {}