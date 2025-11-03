package com.example.sis.repositories;

import com.example.sis.models.GradeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeEntryRepository extends JpaRepository<GradeEntry, Integer> {

    // Tìm tất cả grade entries của một lớp
    List<GradeEntry> findByClassEntity_ClassIdOrderByEntryDateDesc(Integer classId);

    // Tìm grade entries của một lớp và module
    List<GradeEntry> findByClassEntity_ClassIdAndModule_ModuleIdOrderByEntryDateDesc(
            Integer classId, Integer moduleId);

    // Tìm grade entry theo class, module và entry date
    Optional<GradeEntry> findByClassEntity_ClassIdAndModule_ModuleIdAndEntryDate(
            Integer classId, Integer moduleId, LocalDate entryDate);

    // Lấy danh sách các ngày nhập điểm distinct của một module trong lớp
    @Query("""
            SELECT DISTINCT ge.entryDate
            FROM GradeEntry ge
            WHERE ge.classEntity.classId = :classId
              AND ge.module.moduleId = :moduleId
            ORDER BY ge.entryDate DESC
            """)
    List<LocalDate> findDistinctEntryDatesByClassAndModule(Integer classId, Integer moduleId);

    // Kiểm tra xem đã có grade entry cho class, module và date chưa
    boolean existsByClassEntity_ClassIdAndModule_ModuleIdAndEntryDate(
            Integer classId, Integer moduleId, LocalDate entryDate);
}

