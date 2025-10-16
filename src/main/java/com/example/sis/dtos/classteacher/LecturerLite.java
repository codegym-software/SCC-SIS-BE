package com.example.sis.dtos.classteacher;

/**
 * DTO rút gọn cho thông tin giảng viên
 */
public record LecturerLite(
    Long id,
    String fullName,
    String email,
    String avatarUrl
) {}