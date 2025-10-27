package com.example.sis.services;

import com.example.sis.dtos.journal.CreateJournalRequest;
import com.example.sis.dtos.journal.JournalResponse;
import com.example.sis.dtos.journal.UpdateJournalRequest;

import java.util.List;

public interface ClassJournalService {

    /**
     * Tạo nhật ký lớp học mới
     * @param request Dữ liệu nhật ký
     * @param createdByUserId ID người tạo (giảng viên)
     * @return Thông tin nhật ký đã tạo
     */
    JournalResponse createJournal(CreateJournalRequest request, Integer createdByUserId);

    /**
     * Cập nhật nhật ký lớp học
     * @param journalId ID nhật ký cần cập nhật
     * @param request Dữ liệu cập nhật
     * @param updatedByUserId ID người cập nhật (phải là chủ sở hữu hoặc SUPER_ADMIN)
     * @param isSuperAdmin True nếu người cập nhật là SUPER_ADMIN
     * @return Thông tin nhật ký sau khi cập nhật
     */
    JournalResponse updateJournal(Integer journalId, UpdateJournalRequest request, Integer updatedByUserId, boolean isSuperAdmin);

    /**
     * Xóa mềm nhật ký lớp học
     * @param journalId ID nhật ký cần xóa
     * @param deletedByUserId ID người xóa (phải là chủ sở hữu hoặc SUPER_ADMIN)
     * @param isSuperAdmin True nếu người xóa là SUPER_ADMIN
     */
    void softDeleteJournal(Integer journalId, Integer deletedByUserId, boolean isSuperAdmin);

    /**
     * Lấy danh sách nhật ký theo lớp học
     * @param classId ID lớp học
     * @return Danh sách nhật ký
     */
    List<JournalResponse> getJournalsByClass(Integer classId);

    /**
     * Lấy danh sách nhật ký của một giảng viên
     * @param teacherId ID giảng viên
     * @return Danh sách nhật ký
     */
    List<JournalResponse> getJournalsByTeacher(Integer teacherId);

    /**
     * Lấy thông tin một nhật ký
     * @param journalId ID nhật ký
     * @return Thông tin nhật ký
     */
    JournalResponse getJournalById(Integer journalId);

    /**
     * Lấy nhật ký theo lớp và loại
     * @param classId ID lớp học
     * @param journalType Loại nhật ký (PROGRESS, ANNOUNCEMENT, ISSUE, NOTE, OTHER)
     * @return Danh sách nhật ký
     */
    List<JournalResponse> getJournalsByClassAndType(Integer classId, String journalType);
}
