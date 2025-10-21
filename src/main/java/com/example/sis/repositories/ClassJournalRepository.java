package com.example.sis.repositories;

import com.example.sis.models.ClassJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassJournalRepository extends JpaRepository<ClassJournal, Integer> {

    /**
     * Tìm nhật ký theo ID (chưa bị xóa mềm)
     */
    @Query("""
        SELECT j FROM ClassJournal j
        WHERE j.journalId = :journalId
          AND j.deletedAt IS NULL
        """)
    Optional<ClassJournal> findByIdNotDeleted(@Param("journalId") Integer journalId);

    /**
     * Lấy tất cả nhật ký của một lớp (chưa xóa mềm), sắp xếp ngày giảm dần
     */
    @Query("""
        SELECT j FROM ClassJournal j
        WHERE j.classEntity.classId = :classId
          AND j.deletedAt IS NULL
        ORDER BY j.journalDate DESC, j.journalId DESC
        """)
    List<ClassJournal> findAllByClassIdNotDeleted(@Param("classId") Integer classId);

    /**
     * Lấy tất cả nhật ký của một giảng viên (chưa xóa mềm)
     */
    @Query("""
        SELECT j FROM ClassJournal j
        WHERE j.teacher.userId = :teacherId
          AND j.deletedAt IS NULL
        ORDER BY j.journalDate DESC, j.journalId DESC
        """)
    List<ClassJournal> findAllByTeacherIdNotDeleted(@Param("teacherId") Integer teacherId);

    /**
     * Kiểm tra nhật ký có thuộc về giảng viên này không (để validate quyền sửa/xóa)
     */
    @Query("""
        SELECT COUNT(j) > 0 FROM ClassJournal j
        WHERE j.journalId = :journalId
          AND j.teacher.userId = :teacherId
          AND j.deletedAt IS NULL
        """)
    boolean isOwnedByTeacher(@Param("journalId") Integer journalId, 
                             @Param("teacherId") Integer teacherId);

    /**
     * Tìm nhật ký theo lớp và loại nhật ký
     */
    @Query("""
        SELECT j FROM ClassJournal j
        WHERE j.classEntity.classId = :classId
          AND CAST(j.journalType AS string) = :journalType
          AND j.deletedAt IS NULL
        ORDER BY j.journalDate DESC, j.journalId DESC
        """)
    List<ClassJournal> findByClassIdAndType(@Param("classId") Integer classId, 
                                            @Param("journalType") String journalType);
}
