package com.example.sis.repositories;

import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.models.Enrollment;
import com.example.sis.repositories.projections.EnrollmentListView;
import com.example.sis.repositories.projections.EnrollmentStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    @Query(
            value = """
        SELECT
            e.enrollmentId        AS enrollmentId,
            e.classEntity.classId AS classId,
            s.studentId           AS studentId,
            s.fullName            AS studentName,
            s.email               AS studentEmail,
            e.status              AS status,
            e.enrolledAt          AS enrolledAt,
            e.leftAt              AS leftAt,
            e.note                AS note
        FROM Enrollment e
        JOIN e.student s
        WHERE e.classEntity.classId = :classId
          AND e.revokedAt IS NULL
        """,
            countQuery = """
        SELECT COUNT(e)
        FROM Enrollment e
        WHERE e.classEntity.classId = :classId
          AND e.revokedAt IS NULL
        """
    )
    Page<EnrollmentListView> pageByClass(Integer classId, Pageable pageable);

    @Query(
            value = """
        SELECT
            e.enrollmentId        AS enrollmentId,
            e.classEntity.classId AS classId,
            s.studentId           AS studentId,
            s.fullName            AS studentName,
            s.email               AS studentEmail,
            e.status              AS status,
            e.enrolledAt          AS enrolledAt,
            e.leftAt              AS leftAt,
            e.note                AS note
        FROM Enrollment e
        JOIN e.student s
        WHERE e.classEntity.classId = :classId
          AND e.status = :status
          AND e.revokedAt IS NULL
        """,
            countQuery = """
        SELECT COUNT(e)
        FROM Enrollment e
        WHERE e.classEntity.classId = :classId
          AND e.status = :status
          AND e.revokedAt IS NULL
        """
    )
    Page<EnrollmentListView> pageByClassAndStatus(Integer classId, EnrollmentStatus status, Pageable pageable);

    @Query(
            value = """
        SELECT
            e.enrollmentId        AS enrollmentId,
            e.classEntity.classId AS classId,
            s.studentId           AS studentId,
            s.fullName            AS studentName,
            s.email               AS studentEmail,
            e.status              AS status,
            e.enrolledAt          AS enrolledAt,
            e.leftAt              AS leftAt,
            e.note                AS note
        FROM Enrollment e
        JOIN e.student s
        WHERE e.classEntity.classId = :classId
          AND e.effectiveEndDate >= CURRENT_DATE
        """,
            countQuery = """
        SELECT COUNT(e)
        FROM Enrollment e
        WHERE e.classEntity.classId = :classId
          AND e.effectiveEndDate >= CURRENT_DATE
        """
    )
    Page<EnrollmentListView> pageActiveByClass(Integer classId, Pageable pageable);

    // Checks & rules
    boolean existsByClassEntity_ClassIdAndStudent_StudentIdAndEnrolledAt(
            Integer classId, Integer studentId, LocalDate enrolledAt);

    Optional<Enrollment> findByClassEntity_ClassIdAndStudent_StudentIdAndEnrolledAt(
            Integer classId, Integer studentId, LocalDate enrolledAt);

    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.classEntity.classId = :classId
          AND e.student.studentId   = :studentId
          AND e.effectiveEndDate    >= :today
        """)
    List<Enrollment> findActiveByClassAndStudent(Integer classId, Integer studentId, LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Enrollment e WHERE e.enrollmentId = :enrollmentId AND e.classEntity.classId = :classId")
    int deleteByIdAndClass(Integer enrollmentId, Integer classId);

    // >>> Thêm dòng này <<<
    Optional<Enrollment> findByEnrollmentIdAndClassEntity_ClassId(Integer enrollmentId, Integer classId);

    @Query("""
        SELECT e.status AS status, COUNT(e) AS total
        FROM Enrollment e
        WHERE e.classEntity.classId = :classId
        GROUP BY e.status
        """)
    List<EnrollmentStatusCount> countByStatusInClass(Integer classId);
    
    // Methods for StatusManagementService
    List<Enrollment> findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(
            Integer classId, EnrollmentStatus status);
    
    List<Enrollment> findByStudent_StudentIdAndRevokedAtIsNull(Integer studentId);
    
    // Method to get active enrollments by student and status
    List<Enrollment> findByStudent_StudentIdAndStatusAndRevokedAtIsNull(
            Integer studentId, EnrollmentStatus status);
    
    // Method for attendance history - chỉ lấy enrollment chưa bị revoke (active)
    Optional<Enrollment> findFirstByStudent_StudentIdAndClassEntity_ClassIdAndRevokedAtIsNullOrderByEnrolledAtDesc(
            Integer studentId, Integer classId);
    
    // Method to get all enrollments (including revoked) for a student in a class
    List<Enrollment> findByStudent_StudentIdAndClassEntity_ClassId(
            Integer studentId, Integer classId);

    // Method to get all active enrollments in a class (for statistics)
    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.classEntity.classId = :classId
        AND e.revokedAt IS NULL
        ORDER BY e.student.fullName
        """)
    List<Enrollment> findByClassEntity_ClassIdAndDeletedFalse(@org.springframework.data.repository.query.Param("classId") Integer classId);
}
