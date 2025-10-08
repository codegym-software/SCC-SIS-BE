package com.example.sis.repositories;

import com.example.sis.models.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Integer> {

    /**
     * Lấy danh sách Programs đang hoạt động (active) và chưa bị soft delete
     * Sắp xếp theo tên chương trình
     */
    @Query("SELECT p FROM Program p WHERE p.isActive = true AND p.deletedAt IS NULL ORDER BY p.name")
    List<Program> findAllActivePrograms();

    /**
     * Tìm Program theo code và đang hoạt động
     */
    @Query("SELECT p FROM Program p WHERE p.code = :code AND p.isActive = true AND p.deletedAt IS NULL")
    Program findByCodeAndActive(String code);

    /**
     * Lấy danh sách Programs theo category và đang hoạt động
     */
    @Query("SELECT p FROM Program p WHERE p.categoryCode = :categoryCode AND p.isActive = true AND p.deletedAt IS NULL ORDER BY p.name")
    List<Program> findByCategoryCodeAndActive(String categoryCode);

    /**
     * Kiểm tra xem Program có tồn tại và đang hoạt động không
     */
    @Query("SELECT COUNT(p) > 0 FROM Program p WHERE p.programId = :programId AND p.isActive = true AND p.deletedAt IS NULL")
    boolean existsByIdAndActive(Integer programId);
}