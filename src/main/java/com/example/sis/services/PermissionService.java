package com.example.sis.services;

import com.example.sis.dtos.permission.PermissionResponse;
import java.util.List;

/**
 * PermissionService:
 * - Quản lý danh sách quyền trong hệ thống
 * - Chỉ Super Admin mới có quyền xem danh sách quyền
 */
public interface PermissionService {

    /**
     * Lấy danh sách tất cả permissions
     * 
     * @param active - null hoặc true: chỉ lấy permissions đang active
     *               - false: lấy tất cả permissions
     * @return danh sách permissions
     */
    List<PermissionResponse> listPermissions(Boolean active);

    /**
     * Lấy danh sách permissions theo category
     * 
     * @param category - category cần lọc
     * @return danh sách permissions theo category
     */
    List<PermissionResponse> listPermissionsByCategory(String category);

    /**
     * Lấy danh sách tất cả categories
     * 
     * @return danh sách categories
     */
    List<String> listCategories();

    /**
     * Lấy thông tin chi tiết permission theo ID
     * 
     * @param id - ID của permission
     * @return thông tin chi tiết permission
     */
    PermissionResponse getPermissionById(Integer id);
}