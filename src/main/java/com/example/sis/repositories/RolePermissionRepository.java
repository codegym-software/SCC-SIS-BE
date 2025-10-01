package com.example.sis.repositories;

import com.example.sis.models.RolePermission;
import com.example.sis.models.Role;
import com.example.sis.models.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    /**
     * Tìm tất cả permissions của một role
     */
    @Query("SELECT rp FROM RolePermission rp " +
            "JOIN FETCH rp.permission p " +
            "WHERE rp.role.roleId = :roleId AND p.active = true " +
            "ORDER BY p.category, p.name")
    List<RolePermission> findByRoleIdWithPermissions(@Param("roleId") Integer roleId);

    /**
     * Tìm tất cả roles có permission cụ thể
     */
    @Query("SELECT rp FROM RolePermission rp " +
            "JOIN FETCH rp.role r " +
            "WHERE rp.permission.permissionId = :permissionId AND r.active = true " +
            "ORDER BY r.name")
    List<RolePermission> findByPermissionIdWithRoles(@Param("permissionId") Integer permissionId);

    /**
     * Kiểm tra role đã có permission chưa
     */
    boolean existsByRoleAndPermission(Role role, Permission permission);

    /**
     * Tìm RolePermission theo role và permission
     */
    Optional<RolePermission> findByRoleAndPermission(Role role, Permission permission);

    /**
     * Xóa tất cả permissions của một role
     */
    void deleteByRole(Role role);

    /**
     * Xóa permission cụ thể khỏi role
     */
    void deleteByRoleAndPermission(Role role, Permission permission);

    /**
     * Đếm số permissions của một role
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp " +
            "WHERE rp.role.roleId = :roleId AND rp.permission.active = true")
    Long countPermissionsByRoleId(@Param("roleId") Integer roleId);

    /**
     * Tìm tất cả permissions chưa được gán cho role
     */
    @Query("SELECT p FROM Permission p " +
            "WHERE p.active = true " +
            "AND p.permissionId NOT IN (" +
            "    SELECT rp.permission.permissionId FROM RolePermission rp " +
            "    WHERE rp.role.roleId = :roleId" +
            ") " +
            "ORDER BY p.category, p.name")
    List<Permission> findUnassignedPermissionsByRoleId(@Param("roleId") Integer roleId);
}