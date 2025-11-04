package com.example.sis.repositories;

import com.example.sis.models.RolePermission;
import com.example.sis.models.Role;
import com.example.sis.models.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    // ---- Legacy keep (if other code still uses) ----
    @Query("""
        SELECT rp FROM RolePermission rp
        JOIN FETCH rp.permission p
        WHERE rp.role.roleId = :roleId AND p.active = true
        ORDER BY p.category, p.name
        """)
    List<RolePermission> findByRoleIdWithPermissions(@Param("roleId") Integer roleId);

    @Query("""
        SELECT rp FROM RolePermission rp
        JOIN FETCH rp.role r
        WHERE rp.permission.permissionId = :permissionId AND r.active = true
        ORDER BY r.name
        """)
    List<RolePermission> findByPermissionIdWithRoles(@Param("permissionId") Integer permissionId);

    boolean existsByRoleAndPermission(Role role, Permission permission);
    Optional<RolePermission> findByRoleAndPermission(Role role, Permission permission);
    void deleteByRole(Role role);
    void deleteByRoleAndPermission(Role role, Permission permission);

    @Query("""
        SELECT COUNT(rp) FROM RolePermission rp
        WHERE rp.role.roleId = :roleId AND rp.permission.active = true
        """)
    Long countPermissionsByRoleId(@Param("roleId") Integer roleId);

    @Query("""
        SELECT p FROM Permission p
        WHERE p.active = true
          AND p.permissionId NOT IN (
            SELECT rp.permission.permissionId FROM RolePermission rp
            WHERE rp.role.roleId = :roleId
          )
        ORDER BY p.category, p.name
        """)
    List<Permission> findUnassignedPermissionsByRoleId(@Param("roleId") Integer roleId);

    // ---- FIXED: Paged + filtered ASSIGNED (explicit ORDER BY p.name; custom count) ----
    @Query(
            value = """
        SELECT p FROM RolePermission rp
        JOIN rp.permission p
        WHERE rp.role.roleId = :roleId
          AND p.active = true
          AND ( :category IS NULL OR p.category = :category )
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY p.name ASC
      """,
            countQuery = """
        SELECT COUNT(p) FROM RolePermission rp
        JOIN rp.permission p
        WHERE rp.role.roleId = :roleId
          AND p.active = true
          AND ( :category IS NULL OR p.category = :category )
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
          )
      """
    )
    Page<Permission> pageAssignedPermissions(@Param("roleId") Integer roleId,
                                             @Param("q") String q,
                                             @Param("category") String category,
                                             Pageable pageable);

    // ---- Paged + filtered UNASSIGNED (NOT EXISTS; explicit ORDER BY p.name) ----
    @Query(
            value = """
        SELECT p FROM Permission p
        WHERE p.active = true
          AND ( :category IS NULL OR p.category = :category )
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND NOT EXISTS (
            SELECT 1 FROM RolePermission rp
            WHERE rp.role.roleId = :roleId
              AND rp.permission = p
          )
        ORDER BY p.name ASC
      """,
            countQuery = """
        SELECT COUNT(p) FROM Permission p
        WHERE p.active = true
          AND ( :category IS NULL OR p.category = :category )
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND NOT EXISTS (
            SELECT 1 FROM RolePermission rp
            WHERE rp.role.roleId = :roleId
              AND rp.permission = p
          )
      """
    )
    Page<Permission> pageUnassignedPermissions(@Param("roleId") Integer roleId,
                                               @Param("q") String q,
                                               @Param("category") String category,
                                               Pageable pageable);

    // ---- NEW: Revoke MANY by permission IDs (bulk delete; efficient) ----
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM RolePermission rp
        WHERE rp.role.roleId = :roleId
          AND rp.permission.permissionId IN :permissionIds
        """)
    int deleteByRoleIdAndPermissionIds(@Param("roleId") Integer roleId,
                                       @Param("permissionIds") List<Integer> permissionIds);

    // ---- NEW: Get role permissions with permission details for multiple roles (batch query) ----
    @Query("""
        SELECT rp FROM RolePermission rp
        JOIN FETCH rp.permission p
        WHERE rp.role.roleId IN :roleIds AND p.active = true
        ORDER BY rp.role.roleId, p.category, p.name
        """)
    List<RolePermission> findByRoleIdInWithPermission(@Param("roleIds") List<Integer> roleIds);

    // ---- NEW: Check if user has permission via their active roles ----
    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM RolePermission rp
            JOIN rp.role r
            JOIN rp.permission p
            JOIN UserRole ur ON ur.role.roleId = r.roleId
            WHERE ur.user.keycloakUserId = :keycloakUserId
              AND ur.revokedAt IS NULL
              AND r.active = true
              AND p.code = :permissionCode
              AND p.active = true
        ) THEN true ELSE false END
        """)
    boolean userHasPermissionByKeycloakId(@Param("keycloakUserId") String keycloakUserId,
                                          @Param("permissionCode") String permissionCode);
}
