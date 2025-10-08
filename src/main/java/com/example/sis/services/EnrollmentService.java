package com.example.sis.services;

import com.example.sis.dtos.enrollment.EnrollmentRequest;
import com.example.sis.dtos.enrollment.EnrollmentResponse;
import com.example.sis.dtos.enrollment.UpdateEnrollmentRequest;
import com.example.sis.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;

public interface EnrollmentService {

    /** Backlog 1 & 3: danh sách học viên theo lớp + lọc trạng thái */
    Page<EnrollmentResponse> list(Integer classId,
                                  EnrollmentStatus status,
                                  Integer page,
                                  Integer size,
                                  String sort);

    /**
     * Backlog 2: thêm học viên (idempotent)
     * - Nếu đã có ACTIVE trùng (class, student, enrolledAt) → trả bản ghi cũ
     * - Nếu đã có ACTIVE khác enrolledAt → lỗi
     * - Nếu chỉ có bản ghi cũ (không ACTIVE) cùng enrolledAt → trả bản ghi cũ
     */
    EnrollmentResponse enroll(Integer classId, EnrollmentRequest req, Integer currentUserId);

    /**
     * Cập nhật:
     * - Không set leftAt khi status=ACTIVE
     * - Chuyển về ACTIVE → clear leftAt
     * - Sang DROPPED/SUSPENDED và chưa leftAt → auto today
     * - Khi kết thúc → set revokedBy/At
     */
    EnrollmentResponse update(Integer classId,
                              Integer enrollmentId,
                              UpdateEnrollmentRequest req,
                              Integer currentUserId);

    /** Xóa mềm: set DROPPED + leftAt=today (nếu trống) + revokedBy/At + append note (optional) */
    void remove(Integer classId, Integer enrollmentId, Integer currentUserId, String reason);
}
