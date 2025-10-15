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
    boolean existsByKeycloakUserId(String keycloakUserId);
    Optional<User> findByEmail(String email);

    // Find user ID by email (for token lookup)
    @Query("SELECT u.userId FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    // Find user ID by username (for token lookup fallback)
    @Query("SELECT u.userId FROM User u WHERE u.email = :username") // Assuming username maps to email
    Optional<Long> findIdByUsername(@Param("username") String username);

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


    // ALT (nếu text block """ bị lỗi, dùng chuỗi thường):
    // @Query("SELECT DISTINCT u FROM User u LEFT JOIN UserRole ur ON ur.user = u " +
    //        "WHERE u.deletedAt IS NULL " +
    //        "AND ( :centerId IS NULL OR ur.center.centerId = :centerId ) " +
    //        "AND ( ur IS NULL OR ur.revokedAt IS NULL )")
    // List<User> findUsersByCenterId(@Param("centerId") Integer centerId);
}
