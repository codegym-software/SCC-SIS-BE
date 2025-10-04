package com.example.sis.repositories;

import com.example.sis.models.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Integer> {

    /**
     * Lấy danh sách lớp học theo trung tâm và chưa bị soft delete
     * Sắp xếp theo ngày bắt đầu và ID (cho pagination seek-friendly)
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.center.centerId = :centerId AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByCenterIdOrderByStartDateDesc(@Param("centerId") Integer centerId);

    /**
     * Lấy tất cả lớp học chưa bị soft delete (cho Super Admin)
     * Sắp xếp theo ngày bắt đầu và ID
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findAllOrderByStartDateDesc();

    /**
     * Lấy lớp học theo ID và chưa bị soft delete
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.classId = :classId AND c.deletedAt IS NULL")
    Optional<ClassEntity> findByIdAndNotDeleted(@Param("classId") Integer classId);

    /**
     * Lấy danh sách lớp học theo chương trình học
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.program.programId = :programId AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByProgramIdOrderByStartDateDesc(@Param("programId") Integer programId);

    /**
     * Lấy danh sách lớp học theo trạng thái
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.status = :status AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByStatusOrderByStartDateDesc(@Param("status") ClassEntity.ClassStatus status);

    /**
     * Lấy danh sách lớp học theo trung tâm và trạng thái
     */
    @Query("SELECT c FROM ClassEntity c " +
            "WHERE c.center.centerId = :centerId AND c.status = :status AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByCenterIdAndStatusOrderByStartDateDesc(
            @Param("centerId") Integer centerId,
            @Param("status") ClassEntity.ClassStatus status);

    /**
     * Kiểm tra tên lớp đã tồn tại trong trung tâm chưa (cho unique constraint)
     */
    @Query("SELECT COUNT(c) > 0 FROM ClassEntity c " +
            "WHERE c.center.centerId = :centerId AND c.name = :name AND c.deletedAt IS NULL")
    boolean existsByCenterIdAndName(@Param("centerId") Integer centerId, @Param("name") String name);

    /**
     * Kiểm tra tên lớp đã tồn tại trong trung tâm chưa (trừ lớp hiện tại - dùng cho
     * update)
     */
    @Query("SELECT COUNT(c) > 0 FROM ClassEntity c " +
            "WHERE c.center.centerId = :centerId AND c.name = :name " +
            "AND c.classId != :excludeClassId AND c.deletedAt IS NULL")
    boolean existsByCenterIdAndNameExcludingId(
            @Param("centerId") Integer centerId,
            @Param("name") String name,
            @Param("excludeClassId") Integer excludeClassId);
}