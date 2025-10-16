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
            "AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE) " +
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
            "AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)")
    Optional<ClassTeacher> findActiveAssignment(@Param("classId") Integer classId,
            @Param("teacherId") Integer teacherId);

    /**
     * Tìm các lớp mà lecturer đang được gán
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "JOIN FETCH ct.classEntity c " +
            "WHERE ct.teacher.id = :teacherId " +
            "AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE) " +
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

    /**
     * Lấy danh sách lecturers đang active với tìm kiếm và phân trang
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "JOIN FETCH ct.teacher t " +
            "WHERE ct.classEntity.id = :classId " +
            "AND ct.endDate IS NULL " +
            "AND (:q IS NULL OR lower(t.fullName) LIKE lower(concat('%', :q, '%')) OR lower(t.email) LIKE lower(concat('%', :q, '%'))) " +
            "ORDER BY ct.startDate DESC, ct.classTeacherId DESC")
    List<ClassTeacher> findActiveByClassIdWithSearch(@Param("classId") Integer classId,
            @Param("q") String searchQuery);

    /**
     * Đếm số lượng lecturers đang active với tìm kiếm
     */
    @Query("SELECT COUNT(ct) FROM ClassTeacher ct " +
            "JOIN ct.teacher t " +
            "WHERE ct.classEntity.id = :classId " +
            "AND ct.endDate IS NULL " +
            "AND (:q IS NULL OR lower(t.fullName) LIKE lower(concat('%', :q, '%')) OR lower(t.email) LIKE lower(concat('%', :q, '%')))")
    long countActiveByClassIdWithSearch(@Param("classId") Integer classId,
            @Param("q") String searchQuery);

    /**
     * Lấy tất cả lecturers với filter theo status và tìm kiếm
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
            "JOIN FETCH ct.teacher t " +
            "JOIN FETCH ct.assignedBy ab " +
            "LEFT JOIN FETCH ct.revokedBy rb " +
            "WHERE ct.classEntity.id = :classId " +
            "AND (:status = 'all' " +
            "     OR (:status = 'active' AND ct.endDate IS NULL) " +
            "     OR (:status = 'inactive' AND ct.endDate IS NOT NULL)) " +
            "AND (:q IS NULL OR lower(t.fullName) LIKE lower(concat('%', :q, '%')) OR lower(t.email) LIKE lower(concat('%', :q, '%'))) " +
            "ORDER BY ct.startDate DESC, ct.classTeacherId DESC")
    List<ClassTeacher> findAllByClassIdWithFilterAndSearch(@Param("classId") Integer classId,
            @Param("status") String status,
            @Param("q") String searchQuery);

    /**
     * Đếm số lượng tất cả lecturers với filter theo status và tìm kiếm
     */
    @Query("SELECT COUNT(ct) FROM ClassTeacher ct " +
            "JOIN ct.teacher t " +
            "WHERE ct.classEntity.id = :classId " +
            "AND (:status = 'all' " +
            "     OR (:status = 'active' AND ct.endDate IS NULL) " +
            "     OR (:status = 'inactive' AND ct.endDate IS NOT NULL)) " +
            "AND (:q IS NULL OR lower(t.fullName) LIKE lower(concat('%', :q, '%')) OR lower(t.email) LIKE lower(concat('%', :q, '%')))")
    long countAllByClassIdWithFilterAndSearch(@Param("classId") Integer classId,
            @Param("status") String status,
            @Param("q") String searchQuery);

    /**
     * Đếm số lượng giảng viên active hiện tại của lớp
     */
    @Query("SELECT COUNT(ct) FROM ClassTeacher ct " +
            "WHERE ct.classEntity.id = :classId " +
            "AND ct.endDate IS NULL")
    long countActiveByClassId(@Param("classId") Integer classId);

    /**
     * Tìm assignment active theo ID và classId (chỉ những assignment chưa kết thúc)
     */
    @Query("SELECT ct FROM ClassTeacher ct " +
             "WHERE ct.classTeacherId = :id " +
             "AND ct.classEntity.id = :classId " +
             "AND ct.effEndDate >= CURRENT_DATE")
     Optional<ClassTeacher> findByIdAndClassIdAndEffEndDateIsNull(@Param("id") Long id,
                                                                   @Param("classId") Integer classId);

    /**
     * Tìm assignment active theo assignmentId và classId (cho soft revoke)
     */
    @Query("""
      select ct from ClassTeacher ct
      where ct.classTeacherId = :assignmentId and ct.classEntity.id = :classId and ct.effEndDate >= CURRENT_DATE
    """)
    Optional<ClassTeacher> findActiveByIdAndClassId(@Param("assignmentId") Long assignmentId,
                                                    @Param("classId") Integer classId);

    /**
     * Sanity check: lấy thông tin assignment để log
     */
    @Query("""
      SELECT ct.classTeacherId, ct.classEntity.id, ct.endDate, ct.effEndDate
      FROM ClassTeacher ct
      WHERE ct.classTeacherId = :assignmentId
    """)
    Optional<Object[]> findAssignmentInfoById(@Param("assignmentId") Long assignmentId);
}