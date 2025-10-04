package com.example.sis.repositories;

import com.example.sis.models.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

  @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1
            FROM UserRole ur
            WHERE ur.user.userId = :userId
              AND ur.role.roleId = :roleId
              AND COALESCE(ur.center.centerId, -1) = COALESCE(:centerId, -1)
              AND ur.revokedAt IS NULL
        ) THEN true ELSE false END
      """)
  boolean existsActiveAssignment(@Param("userId") Integer userId,
      @Param("roleId") Integer roleId,
      @Param("centerId") Integer centerId);

  @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1
            FROM UserRole ur
            WHERE ur.user.keycloakUserId = :keycloakUserId
              AND ur.role.code = :roleCode
              AND ur.revokedAt IS NULL
        ) THEN true ELSE false END
      """)
  boolean userHasActiveRoleByKeycloakIdAndRoleCode(@Param("keycloakUserId") String keycloakUserId,
      @Param("roleCode") String roleCode);

  @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1
            FROM UserRole ur
            JOIN ur.user u
            JOIN ur.role r
            WHERE u.keycloakUserId = :keycloakUserId
              AND r.code IN :roleCodes
              AND ur.revokedAt IS NULL
              AND ur.center.centerId = :centerId
        ) THEN true ELSE false END
      """)
  boolean userHasAnyActiveRoleAtCenter(@Param("keycloakUserId") String keycloakUserId,
      @Param("roleCodes") List<String> roleCodes,
      @Param("centerId") Integer centerId);

  @Query("""
        SELECT ur
        FROM UserRole ur
        WHERE ur.center.centerId = :centerId
          AND ur.revokedAt IS NULL
      """)
  List<UserRole> findActiveByCenterId(@Param("centerId") Integer centerId);

  @Query("""
        SELECT ur
        FROM UserRole ur
        WHERE ur.center.centerId = :centerId
          AND ur.revokedAt IS NOT NULL
      """)
  List<UserRole> findRevokedByCenterId(@Param("centerId") Integer centerId);

  // NEW: get centerId from userRoleId (for @PreAuthorize guard on revoke)
  @Query("SELECT ur.center.centerId FROM UserRole ur WHERE ur.userRoleId = :userRoleId")
  Integer findCenterIdByUserRoleId(@Param("userRoleId") Integer userRoleId);

  // NEW: get centerId of user by keycloak ID (for current user context)
  @Query("""
        SELECT ur.center.centerId FROM UserRole ur
        WHERE ur.user.keycloakUserId = :keycloakUserId
          AND ur.revokedAt IS NULL
          AND ur.center IS NOT NULL
        ORDER BY ur.assignedAt DESC
        LIMIT 1
      """)
  Integer findCenterIdByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);

  // NEW: paginated list of active user-roles by center (for large datasets)
  @Query("""
        SELECT ur FROM UserRole ur
        WHERE ur.center.centerId = :centerId AND ur.revokedAt IS NULL
      """)
  Page<UserRole> pageActiveByCenterId(@Param("centerId") Integer centerId, Pageable pageable);

  // NEW: check user has a role at center (by user id & role id)
  @Query("""
          SELECT COUNT(ur) > 0
          FROM UserRole ur
          WHERE ur.user.userId = :userId
            AND ur.role.roleId = :roleId
            AND ( (:centerId IS NULL AND ur.center IS NULL) OR ur.center.centerId = :centerId )
            AND ur.revokedAt IS NULL
      """)
  boolean userHasActiveRoleByUserIdAndRoleIdAndCenterId(@Param("userId") Integer userId,
      @Param("roleId") Integer roleId,
      @Param("centerId") Integer centerId);

  // NEW: check by role code
  @Query("""
          SELECT COUNT(ur) > 0
          FROM UserRole ur
          JOIN ur.role r
          WHERE ur.user.userId = :userId
            AND r.code = :roleCode
            AND ( (:centerId IS NULL AND ur.center IS NULL) OR ur.center.centerId = :centerId )
            AND ur.revokedAt IS NULL
      """)
  boolean userHasActiveRoleByUserIdAndRoleCodeAndCenterId(@Param("userId") Integer userId,
      @Param("roleCode") String roleCode,
      @Param("centerId") Integer centerId);

  // NEW: list active roles of a user (ordered by assignedAt desc)
  @Query("""
          SELECT ur FROM UserRole ur
          WHERE ur.user.userId = :userId AND ur.revokedAt IS NULL
          ORDER BY ur.assignedAt DESC
      """)
  List<UserRole> findActiveByUserId(@Param("userId") Integer userId);

  // NEW: bulk soft revoke by ids (efficient)
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
          UPDATE UserRole ur
          SET ur.revokedAt = :now, ur.revokedBy = :by
          WHERE ur.userRoleId IN :ids AND ur.revokedAt IS NULL
      """)
  int markRevokedByIds(@Param("ids") List<Integer> userRoleIds,
      @Param("now") LocalDateTime now,
      @Param("by") String revokedBy);
}
