package com.example.sis.repositories;

import com.example.sis.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    boolean existsByEmail(String email);

    /**
     * Kiểm tra hồ sơ học viên có hợp lệ (đang hoạt động, chưa bị INACTIVE)
     */
    @Query("""
        SELECT COUNT(s) > 0
        FROM Student s
        WHERE s.studentId = :studentId
          AND s.overallStatus <> com.example.sis.enums.OverallStatus.INACTIVE
        """)
    boolean isUsableProfile(Integer studentId);
}
