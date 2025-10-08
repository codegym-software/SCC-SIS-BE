package com.example.sis.repositories;

import com.example.sis.dtos.center.CenterLiteResponse;
import com.example.sis.models.Center;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Integer> {

    // --- giữ nguyên các method sẵn có ---
    boolean existsByCode(String code);
    boolean existsByName(String name);
    List<Center> findAllByOrderByNameAsc();

    @Query("""
        SELECT c FROM Center c
        WHERE c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
    """)
    List<Center> findAllActiveOrderByCreatedAtDesc();

    @Query("""
        SELECT c FROM Center c
        WHERE c.centerId = :id AND c.deletedAt IS NULL
    """)
    Optional<Center> findActiveById(@Param("id") Integer id);

    @Query("""
        SELECT c FROM Center c
        WHERE c.code = :code AND c.deletedAt IS NULL
    """)
    Optional<Center> findActiveByCode(@Param("code") String code);

    @Query("""
        SELECT c FROM Center c
        WHERE c.email = :email AND c.deletedAt IS NULL
    """)
    Optional<Center> findActiveByEmail(@Param("email") String email);

    // --- EXISTS thay cho COUNT(...) > 0 ---
    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM Center c
            WHERE c.code = :code AND c.deletedAt IS NULL
        ) THEN true ELSE false END
    """)
    boolean existsByCodeAndNotDeleted(@Param("code") String code);

    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM Center c
            WHERE c.code = :code AND c.centerId <> :id AND c.deletedAt IS NULL
        ) THEN true ELSE false END
    """)
    boolean existsByCodeAndNotDeletedAndIdNot(@Param("code") String code, @Param("id") Integer id);

    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM Center c
            WHERE c.email = :email AND c.deletedAt IS NULL
        ) THEN true ELSE false END
    """)
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM Center c
            WHERE c.email = :email AND c.centerId <> :id AND c.deletedAt IS NULL
        ) THEN true ELSE false END
    """)
    boolean existsByEmailAndNotDeletedAndIdNot(@Param("email") String email, @Param("id") Integer id);

    @Query("""
        SELECT c FROM Center c
        ORDER BY c.createdAt DESC
    """)
    List<Center> findAllOrderByCreatedAtDesc();

    @Query("""
        SELECT c FROM Center c
        WHERE c.deletedAt IS NOT NULL
        ORDER BY c.deletedAt DESC
    """)
    List<Center> findAllDeactivatedOrderByDeletedAtDesc();

    // --- LITE DROPDOWN: chỉ lấy cột cần (id, code, name); không đụng c.active ---
    @Query("""
        SELECT new com.example.sis.dtos.center.CenterLiteResponse(
            c.centerId, c.code, c.name
        )
        FROM Center c
        WHERE c.deletedAt IS NULL
        ORDER BY c.name ASC
    """)
    List<CenterLiteResponse> findLiteActive();
}
