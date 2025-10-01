package com.example.sis.controllers;

import com.example.sis.constants.RoleCodes;
import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.services.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * GET /api/roles?active=true|false
     * - active = null hoặc không truyền -> mặc định trả các role đang active
     * - active = false -> trả tất cả role
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles(
            @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(roleService.listRoles(active));
    }

    /**
     * GET /api/roles/{id}
     * Lấy thông tin role theo ID
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Integer id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    /**
     * POST /api/roles
     * Tạo role mới
     * Chỉ Super Admin mới được phép tạo
     */
    @PostMapping
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    /**
     * PUT /api/roles/{id}
     * Cập nhật role
     * Chỉ Super Admin mới được phép cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    /**
     * DELETE /api/roles/{id}
     * Xóa role (soft delete)
     * Chỉ Super Admin mới được phép xóa
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok().build();
    }
}
