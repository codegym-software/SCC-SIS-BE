package com.example.sis.dtos.classteacher;

import jakarta.validation.constraints.NotNull;

/**
 * DTO cho từng item trong batch assignment
 */
public record BatchAssignLecturerItem(
    @NotNull(message = "Lecturer ID không được null")
    Integer lecturerId,

    @NotNull(message = "Start date không được null")
    java.time.LocalDate startDate,

    String note
) {}