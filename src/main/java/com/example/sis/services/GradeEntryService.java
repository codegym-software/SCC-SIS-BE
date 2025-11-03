package com.example.sis.services;

import com.example.sis.dtos.grade.CreateGradeEntryRequest;
import com.example.sis.dtos.grade.GradeEntryDetailResponse;
import com.example.sis.dtos.grade.GradeEntryResponse;
import com.example.sis.dtos.grade.StudentGradesResponse;
import com.example.sis.dtos.grade.UpdateGradeRecordsRequest;

import java.time.LocalDate;
import java.util.List;

public interface GradeEntryService {

    /**
     * Tạo đợt nhập điểm mới với danh sách điểm của học viên
     */
    GradeEntryDetailResponse createGradeEntry(CreateGradeEntryRequest request, Integer currentUserId);

    /**
     * Lấy danh sách đợt nhập điểm của một lớp
     * Có thể filter theo moduleId và entryDate
     */
    List<GradeEntryResponse> getGradeEntriesByClass(Integer classId, Integer moduleId, LocalDate entryDate);

    /**
     * Lấy điểm của học viên theo lớp, semester và module
     * - Nếu chưa có moduleId: trả về danh sách modules để chọn
     * - Nếu có moduleId: trả về danh sách điểm của học viên trong lớp, cùng moduleId
     */
    StudentGradesResponse getStudentGrades(Integer classId, Integer semester, Integer moduleId);

    /**
     * Xóa đợt nhập điểm theo classId, moduleId và entryDate
     * Sẽ xóa cả grade_entry và tất cả grade_records trong đợt đó (cascade)
     */
    void deleteGradeEntry(Integer classId, Integer moduleId, LocalDate entryDate);

    /**
     * Sửa điểm học viên trong một đợt nhập điểm
     * Cập nhật các grade_records trong grade_entry được xác định bởi classId, moduleId, entryDate
     */
    GradeEntryDetailResponse updateGradeRecords(UpdateGradeRecordsRequest request, Integer currentUserId);
}

