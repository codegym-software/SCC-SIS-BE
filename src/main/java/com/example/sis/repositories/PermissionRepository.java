package com.example.sis.repositories;

import com.example.sis.models.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    /**
     * Tìm tất cả permissions đang active, sắp xếp theo category và name
     */
    List<Permission> findByActiveTrueOrderByCategoryAscNameAsc();

    /**
     * Tìm tất cả permissions, sắp xếp theo category và name
     */
    List<Permission> findAllByOrderByCategoryAscNameAsc();

    /**
     * Tìm permission theo code
     */
    Optional<Permission> findByCodeAndActiveTrue(String code);

    /**
     * Tìm permissions theo category
     */
    List<Permission> findByCategoryAndActiveTrueOrderByNameAsc(String category);

    /**
     * Tìm tất cả categories của permissions đang active
     */
    @Query("SELECT DISTINCT p.category FROM Permission p WHERE p.active = true ORDER BY p.category")
    List<String> findDistinctCategoriesByActiveTrue();

    /**
     * Kiểm tra permission code đã tồn tại chưa
     */
    boolean existsByCode(String code);

    /**
     * Kiểm tra permission code đã tồn tại chưa (trừ permission hiện tại)
     */
    boolean existsByCodeAndPermissionIdNot(String code, Integer permissionId);
}