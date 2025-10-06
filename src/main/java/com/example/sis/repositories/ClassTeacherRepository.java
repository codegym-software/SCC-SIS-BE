package com.example.sis.repositories;

import com.example.sis.models.ClassTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassTeacherRepository extends JpaRepository<ClassTeacher, Integer> {

    /**
     * Tìm tất cả lecturers hiện tại được gán cho một lớp cụ thể
     * (những assignment đang active - không có end_date hoặc end_date > hôm nay)
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "JOIN FETCH ct.teacher t " +
            "WHERE ct.classEntity.id = :classId " +
            "AND (ct.endDate IS NULL OR ct.endDate > CURRENT_DATE) " +
            "ORDER BY ct.startDate ASC")
    List<ClassTeacher> findActiveByClassId(@Param("classId") Integer classId);

    /**
     * Tìm tất cả lecturers assignments cho một lớp (bao gồm cả inactive)
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "JOIN FETCH ct.teacher t " +
            "WHERE ct.classEntity.id = :classId " +
            "ORDER BY ct.startDate DESC")
    List<ClassTeacher> findAllByClassId(@Param("classId") Integer classId);

    /**
     * Kiểm tra xem lecturer có đang được gán cho lớp không
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "WHERE ct.classEntity.id = :classId " +
            "AND ct.teacher.id = :teacherId " +
            "AND (ct.endDate IS NULL OR ct.endDate > CURRENT_DATE)")
    Optional<ClassTeacher> findActiveAssignment(@Param("classId") Integer classId,
            @Param("teacherId") Integer teacherId);

    /**
     * Tìm các lớp mà lecturer đang được gán
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "JOIN FETCH ct.classEntity c " +
            "WHERE ct.teacher.id = :teacherId " +
            "AND (ct.endDate IS NULL OR ct.endDate > CURRENT_DATE) " +
            "ORDER BY ct.startDate ASC")
    List<ClassTeacher> findActiveByTeacherId(@Param("teacherId") Integer teacherId);

    /**
     * Kiểm tra xem có conflict về thời gian assignment không
     * (dùng để validate trước khi tạo assignment mới)
     */
    @Query("SELECT COUNT(ct) FROM ClassTeacher ct " +
            "WHERE ct.classEntity.id = :classId " +
            "AND ct.teacher.id = :teacherId " +
            "AND ct.startDate <= :endDate " +
            "AND (ct.endDate IS NULL OR ct.endDate >= :startDate)")
    long countConflictingAssignments(@Param("classId") Integer classId,
            @Param("teacherId") Integer teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}