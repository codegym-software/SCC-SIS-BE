package com.example.sis.services;

import com.example.sis.dtos.attendance.*;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    /**
     * Lấy lịch dạy của giảng viên
     * GET /api/attendance-schedules?teacher_id={teacher_id}&from={yyyy-mm-dd}&to={yyyy-mm-dd}
     */
    List<TeacherScheduleResponse> getTeacherSchedule(Integer teacherId, LocalDate from, LocalDate to);

    /**
     * Tạo buổi điểm danh mới
     * POST /api/attendance-sessions
     */
    AttendanceSessionResponse createSession(CreateAttendanceSessionRequest request, Integer currentUserId);

    /**
     * Lấy danh sách buổi điểm danh của một lớp
     * GET /api/classes/{class_id}/attendance-sessions
     */
    List<AttendanceSessionSummaryResponse> getSessionsByClass(Integer classId);

    /**
     * Lấy chi tiết một buổi điểm danh
     * GET /api/attendance-sessions/{session_id}
     */
    AttendanceSessionResponse getSessionDetail(Integer sessionId);

    /**
     * Cập nhật buổi điểm danh
     * PUT /api/attendance-sessions/{session_id}
     */
    AttendanceSessionResponse updateSession(Integer sessionId, UpdateAttendanceSessionRequest request, Integer currentUserId);

    /**
     * Xóa buổi điểm danh (soft delete)
     * DELETE /api/attendance-sessions/{session_id}
     */
    void deleteSession(Integer sessionId, Integer currentUserId);

    /**
     * Lấy lịch sử điểm danh của học viên hiện tại trong một lớp (dựa vào userId từ token)
     * GET /api/attendance/my-attendance/{classId}
     */
    StudentAttendanceHistoryResponse getMyAttendanceByClass(Integer currentUserId, Integer classId);

    /**
     * Lấy lịch sử điểm danh của học viên trong một lớp
     * GET /api/students/{studentId}/classes/{classId}/attendance
     */
    StudentAttendanceHistoryResponse getStudentAttendanceHistory(Integer studentId, Integer classId);
}

