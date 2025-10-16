package com.example.sis.dtos.classteacher;

import java.util.List;

/**
 * Generic response wrapper cho danh sách với tổng số
 */
public record ListResponse<T>(
    long total,
    List<T> items
) {}