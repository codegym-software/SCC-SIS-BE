package com.example.sis.controllers;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.permission.PermissionGroupResponse;
import com.example.sis.services.PermissionService;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("@authz.isSuperAdmin(authentication)")
public class PermissionController {

    private final PermissionService permissionService;
    public PermissionController(PermissionService permissionService) { this.permissionService = permissionService; }

    // ===== CHÍNH: API DUY NHẤT CHO FE =====
    /**
     * GET /api/permissions/groups - API CHÍNH duy nhất cho mọi nhu cầu UI
     *
     * CHỨC NĂNG:
     * - Lấy permissions nhóm theo category với thứ tự cố định
     * - Tìm kiếm fuzzy trên code/name
     * - Lọc theo category cụ thể
     * - Hỗ trợ trạng thái granted cho role permissions
     * - Tổng số quyền = sum(total) của các nhóm
     *
     * PARAMS:
     * - q: fuzzy search on code/name
     * - category: filter by specific category (optional)
     * - activeOnly: true -> only active (default), false -> all
     * - roleId: if provided, include granted status for role permissions
     * - includeEmpty: false -> exclude empty groups (default), true -> include all groups
     */
    @GetMapping("/groups")
    public ResponseEntity<List<PermissionGroupResponse>> groups(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Boolean includeEmpty
    ) {
        return ResponseEntity.ok(permissionService.groups(q, category, activeOnly, roleId, includeEmpty));
    }

    // ===== CÁC API CŨ - DEPRECATED (chỉ giữ để backward compatibility) =====
    /**
     * @deprecated Sử dụng /groups thay thế. API này sẽ bị xóa trong phiên bản tới.
     * Để lấy danh sách phẳng, dùng /groups với includeEmpty=true và không lọc category.
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "50") @Min(1) Integer size,
            @RequestParam(required = false, defaultValue = "name,asc") String sort
    ) {
        return ResponseEntity.ok(permissionService.search(q, category, active, page, size, sort));
    }

    /**
     * @deprecated Thông tin categories đã có trong /groups. API này sẽ bị xóa trong phiên bản tới.
     */
    @Deprecated
    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(permissionService.listCategories());
    }

    /**
     * @deprecated Chỉ giữ nếu có trang chi tiết permission. Hiện tại không cần thiết.
     */
    @Deprecated
    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(permissionService.getById(id));
    }
}
