package com.example.sis.repositories;

import com.example.sis.models.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    // (giữ method cũ nếu nơi khác còn dùng)
    List<Permission> findByActiveTrueOrderByCategoryAscNameAsc();
    List<Permission> findAllByOrderByCategoryAscNameAsc();
    Optional<Permission> findByCodeAndActiveTrue(String code);
    List<Permission> findByCategoryAndActiveTrueOrderByNameAsc(String category);

    @Query("SELECT DISTINCT p.category FROM Permission p WHERE p.active = true ORDER BY p.category")
    List<String> findDistinctCategoriesByActiveTrue();

    boolean existsByCode(String code);
    boolean existsByCodeAndPermissionIdNot(String code, Integer permissionId);

    // NEW: Search + filter + pagination (fast path for big data)
    @Query("""
        SELECT p FROM Permission p
        WHERE
          ( :active IS NULL AND p.active = true OR :active IS NOT NULL AND p.active = :active )
          AND ( :category IS NULL OR p.category = :category )
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """)
    Page<Permission> search(@Param("q") String q,
                            @Param("category") String category,
                            @Param("active") Boolean active,
                            Pageable pageable);

    // NEW: Count active permissions grouped by category
    @Query("SELECT p.category, COUNT(p) FROM Permission p WHERE p.active = true GROUP BY p.category")
    List<Object[]> countActiveGroupByCategory();

    // NEW: Find permissions by role ID (JOIN role_permissions) - only active permissions
    @Query("""
        SELECT p FROM Permission p 
        JOIN RolePermission rp ON p.permissionId = rp.permission.permissionId 
        WHERE rp.role.roleId = :roleId AND p.active = true
        ORDER BY p.category, p.name
        """)
    List<Permission> findByRoleId(@Param("roleId") Integer roleId);

    // NEW: Find permissions by list of permission IDs
    List<Permission> findByPermissionIdIn(Collection<Integer> permissionIds);
}
