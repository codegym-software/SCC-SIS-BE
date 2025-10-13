package com.example.sis.services;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.permission.PermissionGroupResponse;
import java.util.List;

/**
 * Permissions management (Super Admin only).
 *
 * CHÍNH: groups() - Method duy nhất cho mọi nhu cầu UI
 * - Thay thế toàn bộ các method cũ: search, listCategories, getById
 * - Hỗ trợ tìm kiếm, lọc, phân nhóm, đếm tổng, trạng thái granted
 */
public interface PermissionService {

    // ===== CHÍNH: METHOD DUY NHẤT CHO UI =====
    /**
     * Get permissions grouped by category for UI display - METHOD CHÍNH DUY NHẤT
     *
     * CHỨC NĂNG TOÀN DIỆN:
     * - Nhóm permissions theo category với thứ tự cố định từ enum
     * - Tìm kiếm fuzzy trên code/name
     * - Lọc theo category cụ thể
     * - Hỗ trợ trạng thái granted cho role permissions
     * - Tổng số quyền = sum(total) của các nhóm
     * - Thông tin category (label, order) từ PermissionCategory enum
     *
     * PARAMS:
     * - q: fuzzy search on code/name
     * - category: filter by specific category (optional)
     * - activeOnly: true -> only active (default), false -> all
     * - roleId: if provided, include granted status for role permissions
     * - includeEmpty: false -> exclude empty groups (default), true -> include all groups
     */
    List<PermissionGroupResponse> groups(String q, String category, Boolean activeOnly,
                                         Integer roleId, Boolean includeEmpty);

    // ===== CÁC METHOD CŨ - DEPRECATED =====
    /**
     * @deprecated ĐÃ THAY THẾ bởi groups(). Sẽ bị xóa trong phiên bản tới.
     * Để lấy danh sách phẳng, dùng groups() với includeEmpty=true và không lọc category.
     */
    @Deprecated
    List<PermissionResponse> search(String q, String category, Boolean active,
                                    Integer page, Integer size, String sort);

    /**
     * @deprecated Thông tin categories đã có trong groups(). Sẽ bị xóa trong phiên bản tới.
     */
    @Deprecated
    List<String> listCategories();

    /**
     * @deprecated Chỉ cần thiết nếu có trang chi tiết permission. Hiện tại không cần.
     */
    @Deprecated
    PermissionResponse getById(Integer id);

    // ---- Legacy (kept for backward compatibility) ----
    @Deprecated
    List<PermissionResponse> listPermissions(Boolean active);

    @Deprecated
    List<PermissionResponse> listPermissionsByCategory(String category);
}
