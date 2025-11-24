// src/main/java/com/example/sis/repository/UserRepository.java
package com.example.sis.repositories;

import com.example.sis.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;       // <-- import Query
import org.springframework.data.repository.query.Param; // <-- import Param
import java.util.List;                                 // <-- import List
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmail(String email);

    /**
     * Kiểm tra email đã tồn tại (case-insensitive, không bao gồm người dùng đã xóa mềm)
     */
    @Query("""
        SELECT COUNT(u) > 0
        FROM User u
        WHERE LOWER(u.email) = LOWER(:email)
          AND u.deletedAt IS NULL
        """)
    boolean existsByEmailIgnoreCase(@Param("email") String email);
    boolean existsByKeycloakUserId(String keycloakUserId);
    Optional<User> findByEmail(String email);

    // Find user ID by email (for token lookup)
    @Query("SELECT u.userId FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    // Find user ID by username (for token lookup fallback)
    @Query("SELECT u.userId FROM User u WHERE u.email = :username") // Assuming username maps to email
    Optional<Long> findIdByUsername(@Param("username") String username);

    // Find user ID by Keycloak user ID (sub claim)
    @Query("SELECT u.userId FROM User u WHERE u.keycloakUserId = :keycloakUserId")
    Optional<Integer> findIdByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);

    // Find user by Keycloak user ID (sub claim)
    Optional<User> findByKeycloakUserId(String keycloakUserId);

    // Java 17 text block OK; nếu IDE kêu, dùng bản ALT ở dưới.
    @Query("""
    SELECT DISTINCT u
    FROM User u
    JOIN UserRole ur ON ur.user = u
    WHERE u.deletedAt IS NULL
      AND ( :centerId IS NULL OR ur.center.centerId = :centerId )
      AND ur.revokedAt IS NULL
""")
    List<User> findUsersByCenterId(@Param("centerId") Integer centerId);

    /**
     * Tìm giảng viên available để phân công cho lớp
     * (thuộc center, có role LECTURER, chưa có assignment active trong lớp)
     */
    @Query("""
    SELECT DISTINCT u
    FROM User u
    JOIN UserRole ur ON ur.user = u
    JOIN Role r ON ur.role = r
    WHERE u.deletedAt IS NULL
      AND ur.center.centerId = :centerId
      AND r.code = 'LECTURER'
      AND ur.revokedAt IS NULL
      AND NOT EXISTS (
        SELECT 1 FROM ClassTeacher ct
        WHERE ct.classEntity.classId = :classId
          AND ct.teacher.userId = u.userId
          AND ct.endDate IS NULL
      )
      AND (:q IS NULL OR lower(u.fullName) LIKE lower(concat('%', :q, '%')) OR lower(u.email) LIKE lower(concat('%', :q, '%')))
    ORDER BY u.fullName ASC
""")
    List<User> findAvailableLecturersByCenterAndClass(@Param("centerId") Integer centerId,
                                                      @Param("classId") Integer classId,
                                                      @Param("q") String searchQuery);

    /**
     * Tìm tất cả admin và manager của một center để gửi thông báo activity log
     * (SUPER_ADMIN + CENTER_MANAGER + ACADEMIC_STAFF)
     */
    @Query("""
    SELECT DISTINCT u
    FROM User u
    JOIN UserRole ur ON ur.user = u
    JOIN Role r ON ur.role = r
    WHERE u.deletedAt IS NULL
      AND ur.revokedAt IS NULL
      AND (
        r.code = 'SUPER_ADMIN'
        OR (r.code IN ('CENTER_MANAGER', 'ACADEMIC_STAFF') AND ur.center.centerId = :centerId)
      )
""")
    List<User> findAdminsByCenterId(@Param("centerId") Integer centerId);

    // ALT (nếu text block """ bị lỗi, dùng chuỗi thường):
    // @Query("SELECT DISTINCT u FROM User u LEFT JOIN UserRole ur ON ur.user = u " +
    //        "WHERE u.deletedAt IS NULL " +
    //        "AND ( :centerId IS NULL OR ur.center.centerId = :centerId ) " +
    //        "AND ( ur IS NULL OR ur.revokedAt IS NULL )")
    // List<User> findUsersByCenterId(@Param("centerId") Integer centerId);
}
