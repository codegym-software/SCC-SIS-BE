package com.example.sis.dtos.classteacher;

import java.util.List;

/**
 * DTO cho response của batch assignment
 */
public record BatchAssignLecturerResponse(
    int created,
    List<Integer> skipped
) {}