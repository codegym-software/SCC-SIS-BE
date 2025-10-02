package com.example.sis.repositories;

import com.example.sis.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
