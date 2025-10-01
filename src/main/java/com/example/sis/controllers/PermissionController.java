package com.example.sis.controllers;

import com.example.sis.constants.RoleCodes;
import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.services.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * GET /api/permissions?active=true|false
     * Xem danh sách tất cả các Quyền (Permissions) có trong hệ thống
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(permissionService.listPermissions(active));
    }

    /**
     * GET /api/permissions/categories
     * Lấy danh sách các categories của permissions
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping("/categories")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<String>> listCategories() {
        return ResponseEntity.ok(permissionService.listCategories());
    }

    /**
     * GET /api/permissions/by-category?category=USER
     * Lấy danh sách permissions theo category
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping("/by-category")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<PermissionResponse>> listPermissionsByCategory(
            @RequestParam String category) {
        return ResponseEntity.ok(permissionService.listPermissionsByCategory(category));
    }

    /**
     * GET /api/permissions/{id}
     * Xem chi tiết một permission theo ID
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable Integer id) {
        return ResponseEntity.ok(permissionService.getPermissionById(id));
    }
}