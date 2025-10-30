package com.example.sis.services;

import com.example.sis.dtos.module.CreateModuleRequest;
import com.example.sis.dtos.module.ModuleResponse;
import com.example.sis.dtos.module.UpdateModuleRequest;

import java.util.List;

public interface ModuleService {

    /**
     * Tạo module mới
     * @param request Dữ liệu module
     * @param createdBy User ID người tạo
     * @return ModuleResponse
     */
    ModuleResponse createModule(CreateModuleRequest request, Integer createdBy);

    /**
     * Lấy module theo ID
     * @param moduleId ID của module
     * @return ModuleResponse
     */
    ModuleResponse getModuleById(Integer moduleId);

    /**
     * Lấy danh sách modules của program
     * @param programId ID của program
     * @return Danh sách modules
     */
    List<ModuleResponse> getModulesByProgramId(Integer programId);

    /**
     * Cập nhật module
     * @param moduleId ID của module
     * @param request Dữ liệu cập nhật
     * @param updatedBy User ID người cập nhật
     * @return ModuleResponse
     */
    ModuleResponse updateModule(Integer moduleId, UpdateModuleRequest request, Integer updatedBy);

    /**
     * Xóa module (soft delete)
     * @param moduleId ID của module
     */
    void deleteModule(Integer moduleId);

    /**
     * Tìm kiếm modules theo từ khóa
     * @param programId ID của program
     * @param query Từ khóa tìm kiếm
     * @return Danh sách modules
     */
    List<ModuleResponse> searchModules(Integer programId, String query);

    /**
     * Lấy modules theo level
     * @param programId ID của program
     * @param level Level (Beginner/Intermediate/Advanced)
     * @return Danh sách modules
     */
    List<ModuleResponse> getModulesByLevel(Integer programId, String level);

    /**
     * Lấy modules bắt buộc của program
     * @param programId ID của program
     * @return Danh sách modules bắt buộc
     */
    List<ModuleResponse> getMandatoryModules(Integer programId);

    /**
     * Sắp xếp lại thứ tự module trong program (theo moduleId)
     * @param moduleId ID của module cần di chuyển
     * @param newSequenceOrder Vị trí mới
     * @param updatedBy User ID người cập nhật
     * @param isAdminOrSA true nếu người dùng là ADMIN/SA (có thể sắp xếp tất cả), false nếu là Student
     * @return Danh sách modules sau khi sắp xếp lại
     */
    List<ModuleResponse> reorderModule(Integer moduleId, Integer newSequenceOrder, Integer updatedBy, boolean isAdminOrSA);

    /**
     * Sắp xếp lại thứ tự module trong program (theo sequenceOrder)
     * @param programId ID của program
     * @param currentSequenceOrder Vị trí hiện tại của module
     * @param newSequenceOrder Vị trí mới
     * @param updatedBy User ID người cập nhật
     * @param isAdminOrSA true nếu người dùng là ADMIN/SA (có thể sắp xếp tất cả), false nếu là Student
     * @return Danh sách modules sau khi sắp xếp lại
     */
    List<ModuleResponse> reorderModuleBySequenceOrder(Integer programId, Integer currentSequenceOrder, Integer newSequenceOrder, Integer updatedBy, boolean isAdminOrSA);

    /**
     * Tự động sắp xếp lại sequenceOrder cho tất cả modules trong program
     * Sắp xếp theo semester tăng dần, sau đó theo moduleId
     * @param programId ID của program
     * @return Danh sách modules sau khi sắp xếp lại
     */
    List<ModuleResponse> resequenceModules(Integer programId);

    /**
     * Gắn tài liệu (resourceUrl) vào module
     * @param moduleId ID của module
     * @param resourceUrl URL của tài liệu (YouTube, Drive, uploaded file, etc.)
     * @param updatedBy User ID người cập nhật
     * @return ModuleResponse
     */
    ModuleResponse attachResource(Integer moduleId, String resourceUrl, Integer updatedBy);

    /**
     * Xóa tài liệu khỏi module (set resourceUrl = null)
     * @param moduleId ID của module
     * @param updatedBy User ID người cập nhật
     * @return ModuleResponse
     */
    ModuleResponse removeResource(Integer moduleId, Integer updatedBy);

    /**
     * Xóa 1 tài liệu cụ thể theo URL khỏi danh sách resources
     * @param moduleId ID của module
     * @param resourceUrl URL của tài liệu cần xóa
     * @param updatedBy User ID người cập nhật
     * @return ModuleResponse
     */
    ModuleResponse removeResourceByUrl(Integer moduleId, String resourceUrl, Integer updatedBy);

    /**
     * Lấy modules theo semester của program
     * @param programId ID của program
     * @param semester Số học kỳ (1, 2, 3, 4...)
     * @return Danh sách modules trong semester đó
     */
    List<ModuleResponse> getModulesBySemester(Integer programId, Integer semester);
}




