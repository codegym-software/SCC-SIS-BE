package com.example.sis.repositories;

import com.example.sis.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

  /**
   * Tìm kiếm học viên theo tên hoặc email (không bao gồm học viên đã xóa mềm)
   */
  @Query("""
      SELECT s FROM Student s
      WHERE s.deletedAt IS NULL
        AND (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY s.fullName ASC
      """)
  List<Student> searchByNameOrEmail(@Param("keyword") String keyword);

  /**
   * Lấy tất cả học viên chưa bị xóa mềm
   */
  @Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL ORDER BY s.createdAt DESC")
  List<Student> findAllActiveStudents();

  /**
   * Tìm studentId từ userId
   */
  @Query("SELECT s.studentId FROM Student s WHERE s.user.userId = :userId AND s.deletedAt IS NULL")
  Integer findStudentIdByUserId(@Param("userId") Integer userId);
}
