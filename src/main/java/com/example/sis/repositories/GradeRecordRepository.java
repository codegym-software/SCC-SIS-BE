package com.example.sis.repositories;

import com.example.sis.models.GradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRecordRepository extends JpaRepository<GradeRecord, Integer> {

    // Tìm tất cả grade records của một grade entry
    List<GradeRecord> findByGradeEntry_GradeEntryIdOrderByStudent_FullName(Integer gradeEntryId);

    // Tìm grade record theo grade entry và student
    Optional<GradeRecord> findByGradeEntry_GradeEntryIdAndStudent_StudentId(
            Integer gradeEntryId, Integer studentId);

    // Kiểm tra xem đã có grade record cho grade entry và student chưa
    boolean existsByGradeEntry_GradeEntryIdAndStudent_StudentId(
            Integer gradeEntryId, Integer studentId);

    // Tìm grade records theo student (để xem lịch sử điểm của một học viên)
    List<GradeRecord> findByStudent_StudentIdOrderByGradeEntry_EntryDateDesc(Integer studentId);

    // Tìm tất cả grade records của một student
    List<GradeRecord> findByStudent_StudentId(Integer studentId);

    // Tìm tất cả grade records của một student trong một class cụ thể
    @org.springframework.data.jpa.repository.Query("""
        SELECT gr FROM GradeRecord gr
        WHERE gr.student.studentId = :studentId
        AND gr.gradeEntry.classEntity.classId = :classId
        """)
    List<GradeRecord> findByStudentIdAndClassId(Integer studentId, Integer classId);
}

