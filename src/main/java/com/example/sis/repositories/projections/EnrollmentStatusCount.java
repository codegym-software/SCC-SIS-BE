package com.example.sis.repositories.projections;

import com.example.sis.enums.EnrollmentStatus;

/** Projection đếm số lượng theo trạng thái trong lớp */
public interface EnrollmentStatusCount {
    EnrollmentStatus getStatus();
    long getTotal();
}
