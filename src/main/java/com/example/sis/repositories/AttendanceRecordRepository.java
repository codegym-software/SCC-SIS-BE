package com.example.sis.repositories;

import com.example.sis.dtos.attendance.AttendanceRecordResponse;
import com.example.sis.enums.AttendanceStatus;
import com.example.sis.models.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Integer> {

    /**
     * Lấy tất cả bản ghi điểm danh của một buổi điểm danh
     */
    @Query("""
        SELECT new com.example.sis.dtos.attendance.AttendanceRecordResponse(
            ar.recordId,
            ar.session.sessionId,
            ar.enrollment.enrollmentId,
            ar.student.studentId,
            ar.student.fullName,
            CONCAT('SV', LPAD(CAST(ar.student.studentId AS string), 3, '0')),
            ar.student.email,
            ar.status,
            ar.notes
        )
        FROM AttendanceRecord ar
        WHERE ar.session.sessionId = :sessionId
        AND ar.deleted = false
        ORDER BY ar.student.fullName
        """)
    List<AttendanceRecordResponse> findBySessionId(@Param("sessionId") Integer sessionId);

    /**
     * Tìm bản ghi theo session và student
     */
    Optional<AttendanceRecord> findBySession_SessionIdAndStudent_StudentIdAndDeletedFalse(
            Integer sessionId, Integer studentId);

    /**
     * Đếm số học viên theo trạng thái trong một buổi điểm danh
     */
    @Query("""
        SELECT COUNT(ar) FROM AttendanceRecord ar
        WHERE ar.session.sessionId = :sessionId
        AND ar.status = :status
        AND ar.deleted = false
        """)
    long countBySessionIdAndStatus(@Param("sessionId") Integer sessionId, @Param("status") AttendanceStatus status);

    /**
     * Lấy tất cả bản ghi của một học viên
     */
    @Query("""
        SELECT ar FROM AttendanceRecord ar
        WHERE ar.student.studentId = :studentId
        AND ar.deleted = false
        ORDER BY ar.createdAt DESC
        """)
    List<AttendanceRecord> findByStudentId(@Param("studentId") Integer studentId);

    /**
     * Lấy lịch sử điểm danh của một học viên trong một lớp (thông qua enrollment)
     */
    List<AttendanceRecord> findByEnrollment_EnrollmentIdAndDeletedFalseOrderBySession_AttendanceDateDesc(Integer enrollmentId);

    /**
     * Lấy TẤT CẢ lịch sử điểm danh của một học viên trong một lớp cụ thể
     * (không phụ thuộc vào enrollment_id, lấy theo student_id + class_id)
     * Bao gồm cả buổi điểm danh trong tương lai để học viên có thể xem
     */
    @Query("""
        SELECT ar FROM AttendanceRecord ar
        JOIN ar.session s
        WHERE ar.student.studentId = :studentId
          AND s.classEntity.classId = :classId
          AND ar.deleted = false
        ORDER BY s.attendanceDate DESC
        """)
    List<AttendanceRecord> findByStudentIdAndClassIdOrderByAttendanceDateDesc(
        @Param("studentId") Integer studentId,
        @Param("classId") Integer classId
    );

    /**
     * Lấy tất cả bản ghi điểm danh theo sessionId
     */
    List<AttendanceRecord> findBySession_SessionIdAndDeletedFalse(Integer sessionId);
}

