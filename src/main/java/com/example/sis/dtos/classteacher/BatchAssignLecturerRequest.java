package com.example.sis.dtos.classteacher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO cho batch assignment của giảng viên
 */
public record BatchAssignLecturerRequest(
    @NotEmpty(message = "Items list không được trống")
    @Size(max = 10, message = "Không được gán quá 10 giảng viên cùng lúc")
    @Valid
    List<BatchAssignLecturerItem> items
) {}