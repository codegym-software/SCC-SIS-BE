package com.example.sis.repositories;

import com.example.sis.models.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // có thể bỏ, nhưng để cũng không sao
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Phân trang tránh trả quá nhiều bản ghi
    Page<Role> findAllByOrderByNameAsc(Pageable pageable);
    Page<Role> findByActiveTrueOrderByNameAsc(Pageable pageable);

    boolean existsByCode(String code);

    // Search + filter active + pagination
    @Query("""
        SELECT r FROM Role r
        WHERE
          (
            (:active IS NULL AND r.active = true)
            OR (:active IS NOT NULL AND r.active = :active)
          )
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(r.code) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """)
    Page<Role> search(@Param("q") String q,
                      @Param("active") Boolean active,
                      Pageable pageable);

    // get-by-id chỉ khi active
    Optional<Role> findByRoleIdAndActiveTrue(Integer roleId);

    // find active role by ID (alternative naming for consistency)
    default Optional<Role> findActiveById(Integer roleId) {
        return findByRoleIdAndActiveTrue(roleId);
    }

    // Lấy tất cả active roles sắp xếp theo createdAt tăng dần
    List<Role> findByActiveTrueOrderByCreatedAtAsc();

    // Tìm ID của role active theo code
    @Query("SELECT r.roleId FROM Role r WHERE r.code = :code AND r.active = true")
    Optional<Integer> findIdByCode(@Param("code") String code);
}
