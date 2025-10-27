package com.example.sis.repositories;

import com.example.sis.models.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {

    /**
     * Tìm module theo ID (chỉ active, chưa bị xóa)
     */
    @Query("SELECT m FROM Module m WHERE m.moduleId = :id AND m.isActive = true AND m.deletedAt IS NULL")
    Optional<Module> findActiveById(@Param("id") Integer id);

    /**
     * Lấy tất cả modules của một program (active, chưa bị xóa)
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId AND m.isActive = true AND m.deletedAt IS NULL ORDER BY m.sequenceOrder")
    List<Module> findByProgramIdAndActiveOrderBySequence(@Param("programId") Integer programId);

    /**
     * Lấy tất cả modules của một program (bao gồm cả inactive)
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId AND m.deletedAt IS NULL ORDER BY m.sequenceOrder")
    List<Module> findByProgramIdOrderBySequence(@Param("programId") Integer programId);

    /**
     * Kiểm tra module code đã tồn tại trong program chưa
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Module m " +
           "WHERE m.programId = :programId AND m.code = :code AND m.deletedAt IS NULL")
    boolean existsByProgramIdAndCode(@Param("programId") Integer programId, @Param("code") String code);

    /**
     * Kiểm tra sequence order đã được sử dụng trong program chưa
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Module m " +
           "WHERE m.programId = :programId AND m.sequenceOrder = :sequenceOrder AND m.deletedAt IS NULL")
    boolean existsByProgramIdAndSequenceOrder(@Param("programId") Integer programId, 
                                               @Param("sequenceOrder") Integer sequenceOrder);

    /**
     * Kiểm tra module code đã tồn tại (ngoại trừ module hiện tại)
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Module m " +
           "WHERE m.programId = :programId AND m.code = :code AND m.moduleId != :moduleId AND m.deletedAt IS NULL")
    boolean existsByProgramIdAndCodeExcludingId(@Param("programId") Integer programId, 
                                                  @Param("code") String code, 
                                                  @Param("moduleId") Integer moduleId);

    /**
     * Kiểm tra sequence order đã được sử dụng (ngoại trừ module hiện tại)
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Module m " +
           "WHERE m.programId = :programId AND m.sequenceOrder = :sequenceOrder AND m.moduleId != :moduleId AND m.deletedAt IS NULL")
    boolean existsByProgramIdAndSequenceOrderExcludingId(@Param("programId") Integer programId, 
                                                           @Param("sequenceOrder") Integer sequenceOrder, 
                                                           @Param("moduleId") Integer moduleId);

    /**
     * Lấy module theo program và code
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId AND m.code = :code AND m.deletedAt IS NULL")
    Optional<Module> findByProgramIdAndCode(@Param("programId") Integer programId, @Param("code") String code);

    /**
     * Tìm kiếm modules theo tên hoặc mã (fuzzy search)
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId " +
           "AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(m.code) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND m.isActive = true AND m.deletedAt IS NULL " +
           "ORDER BY m.sequenceOrder")
    List<Module> searchByProgramIdAndQuery(@Param("programId") Integer programId, @Param("query") String query);

    /**
     * Lấy modules theo level
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId AND m.level = :level " +
           "AND m.isActive = true AND m.deletedAt IS NULL ORDER BY m.sequenceOrder")
    List<Module> findByProgramIdAndLevel(@Param("programId") Integer programId, @Param("level") String level);

    /**
     * Lấy modules bắt buộc của program
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId AND m.isMandatory = true " +
           "AND m.isActive = true AND m.deletedAt IS NULL ORDER BY m.sequenceOrder")
    List<Module> findMandatoryByProgramId(@Param("programId") Integer programId);

    /**
     * Đếm số modules trong program
     */
    @Query("SELECT COUNT(m) FROM Module m WHERE m.programId = :programId AND m.deletedAt IS NULL")
    long countByProgramId(@Param("programId") Integer programId);

    /**
     * Lấy module theo program và sequence order
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId AND m.sequenceOrder = :sequenceOrder AND m.deletedAt IS NULL")
    Optional<Module> findByProgramIdAndSequenceOrder(@Param("programId") Integer programId, 
                                                      @Param("sequenceOrder") Integer sequenceOrder);

    /**
     * Lấy tất cả modules trong khoảng sequence order (dùng cho reorder)
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId " +
           "AND m.sequenceOrder BETWEEN :minOrder AND :maxOrder " +
           "AND m.deletedAt IS NULL ORDER BY m.sequenceOrder")
    List<Module> findByProgramIdAndSequenceOrderBetween(@Param("programId") Integer programId,
                                                         @Param("minOrder") Integer minOrder,
                                                         @Param("maxOrder") Integer maxOrder);

    /**
     * Lấy tất cả modules theo semester và program (active, chưa bị xóa)
     * Dùng để kiểm tra khi reorder module
     */
    @Query("SELECT m FROM Module m WHERE m.semester = :semester AND m.programId = :programId " +
           "AND m.isActive = true AND m.deletedAt IS NULL ORDER BY m.sequenceOrder")
    List<Module> findBySemesterAndProgramId(@Param("semester") Integer semester, 
                                            @Param("programId") Integer programId);

    /**
     * Lấy tất cả modules của program sắp xếp theo semester và moduleId
     * Dùng cho việc tự động sắp xếp lại sequenceOrder
     */
    @Query("SELECT m FROM Module m WHERE m.programId = :programId " +
           "AND m.isActive = true AND m.deletedAt IS NULL " +
           "ORDER BY m.semester ASC, m.moduleId ASC")
    List<Module> findByProgramIdOrderBySemesterAscModuleIdAsc(@Param("programId") Integer programId);
}




