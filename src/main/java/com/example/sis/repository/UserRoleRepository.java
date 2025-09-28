// src/main/java/com/example/sis/repository/UserRoleRepository.java
package com.example.sis.repository;

import com.example.sis.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

  @Query("""
          SELECT COUNT(ur) > 0
          FROM UserRole ur
          WHERE ur.user.userId = :userId
            AND ur.role.roleId = :roleId
            AND ( (:centerId IS NULL AND ur.center IS NULL) OR (ur.center.centerId = :centerId) )
            AND ur.revokedAt IS NULL
      """)
  boolean existsActiveAssignment(@Param("userId") Integer userId,
      @Param("roleId") Integer roleId,
      @Param("centerId") Integer centerId);

  @Query("""
          SELECT COUNT(ur) > 0
          FROM UserRole ur
          JOIN ur.user u
          JOIN ur.role r
          WHERE u.keycloakUserId = :keycloakUserId
            AND r.code = :roleCode
            AND ur.revokedAt IS NULL
      """)
  boolean userHasActiveRoleByKeycloakIdAndRoleCode(@Param("keycloakUserId") String keycloakUserId,
      @Param("roleCode") String roleCode);

  // NEW: có bất kỳ role trong danh sách ở 1 center cụ thể không
  @Query("""
          SELECT COUNT(ur) > 0
          FROM UserRole ur
          JOIN ur.user u
          JOIN ur.role r
          WHERE u.keycloakUserId = :keycloakUserId
            AND r.code IN :roleCodes
            AND ur.revokedAt IS NULL
            AND ur.center.centerId = :centerId
      """)
  boolean userHasAnyActiveRoleAtCenter(@Param("keycloakUserId") String keycloakUserId,
      @Param("roleCodes") List<String> roleCodes,
      @Param("centerId") Integer centerId);

  // NEW: Tìm tất cả user-roles đang active của một center
  @Query("""
          SELECT ur FROM UserRole ur
          WHERE ur.center.centerId = :centerId
            AND ur.revokedAt IS NULL
      """)
  List<UserRole> findActiveByCenterId(@Param("centerId") Integer centerId);
}
