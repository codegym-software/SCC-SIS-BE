package com.example.sis.repositories;

import com.example.sis.models.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Integer> {

    /**
     * Tìm buổi điểm danh theo lớp và ngày
     */
    Optional<AttendanceSession> findByClassEntity_ClassIdAndAttendanceDateAndDeletedFalse(
            Integer classId, LocalDate attendanceDate);

    /**
     * Lấy danh sách buổi điểm danh của một lớp
     */
    List<AttendanceSession> findByClassEntity_ClassIdAndDeletedFalseOrderByAttendanceDateDesc(
            Integer classId);

    /**
     * Lấy danh sách buổi điểm danh theo giảng viên trong khoảng thời gian
     */
    @Query("""
        SELECT s FROM AttendanceSession s
        WHERE s.teacher.userId = :teacherId
        AND s.attendanceDate >= :fromDate
        AND s.attendanceDate <= :toDate
        AND s.deleted = false
        ORDER BY s.attendanceDate DESC, s.sessionId DESC
        """)
    List<AttendanceSession> findByTeacherAndDateRange(
            @Param("teacherId") Integer teacherId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Kiểm tra xem lớp đã được điểm danh trong ngày chưa
     */
    boolean existsByClassEntity_ClassIdAndAttendanceDateAndDeletedFalse(
            Integer classId, LocalDate attendanceDate);

    /**
     * Đếm số buổi điểm danh đã thực hiện của một giảng viên
     */
    @Query("""
        SELECT COUNT(s) FROM AttendanceSession s
        WHERE s.teacher.userId = :teacherId
        AND s.attendanceDate = :date
        AND s.deleted = false
        """)
    long countByTeacherAndDate(@Param("teacherId") Integer teacherId, @Param("date") LocalDate date);

    /**
     * Lấy danh sách buổi điểm danh của một lớp trong khoảng thời gian
     * (Dùng để hiển thị lịch sử sessions kể cả khi studyDays thay đổi)
     */
    List<AttendanceSession> findByClassEntity_ClassIdAndAttendanceDateBetweenAndDeletedFalse(
            Integer classId, LocalDate fromDate, LocalDate toDate);
}

