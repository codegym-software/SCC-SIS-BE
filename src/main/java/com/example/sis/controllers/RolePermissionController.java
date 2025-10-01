package com.example.sis.controllers;

import com.example.sis.constants.RoleCodes;
import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;
import com.example.sis.services.RolePermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * GET /api/role-permissions/roles/{roleId}/permissions
     * Lấy danh sách permissions của một role
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByRole(@PathVariable Integer roleId) {
        return ResponseEntity.ok(rolePermissionService.getPermissionsByRoleId(roleId));
    }

    /**
     * GET /api/role-permissions/roles/{roleId}/unassigned-permissions
     * Lấy danh sách permissions chưa được gán cho role
     * Chỉ Super Admin mới được phép xem
     */
    @GetMapping("/roles/{roleId}/unassigned-permissions")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<PermissionResponse>> getUnassignedPermissionsByRole(@PathVariable Integer roleId) {
        return ResponseEntity.ok(rolePermissionService.getUnassignedPermissionsByRoleId(roleId));
    }

    /**
     * POST /api/role-permissions
     * Gán permission cho role
     * Chỉ Super Admin mới được phép thực hiện
     */
    @PostMapping
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<RolePermissionResponse> assignPermissionToRole(
            @Valid @RequestBody RolePermissionRequest request,
            Authentication authentication) {

        final Jwt jwt = (Jwt) authentication.getPrincipal();
        final String grantedBy = jwt.getSubject();

        return ResponseEntity.ok(rolePermissionService.assignPermissionToRole(request, grantedBy));
    }

    /**
     * POST /api/role-permissions/roles/{roleId}/permissions/batch
     * Gán nhiều permissions cho role cùng lúc
     * Chỉ Super Admin mới được phép thực hiện
     */
    @PostMapping("/roles/{roleId}/permissions/batch")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<RolePermissionResponse>> assignMultiplePermissionsToRole(
            @PathVariable Integer roleId,
            @RequestBody List<Integer> permissionIds,
            Authentication authentication) {

        final Jwt jwt = (Jwt) authentication.getPrincipal();
        final String grantedBy = jwt.getSubject();

        return ResponseEntity
                .ok(rolePermissionService.assignMultiplePermissionsToRole(roleId, permissionIds, grantedBy));
    }

    /**
     * DELETE /api/role-permissions/roles/{roleId}/permissions/{permissionId}
     * Thu hồi permission từ role
     * Chỉ Super Admin mới được phép thực hiện
     */
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<Void> revokePermissionFromRole(@PathVariable Integer roleId,
            @PathVariable Integer permissionId) {
        rolePermissionService.revokePermissionFromRole(roleId, permissionId);
        return ResponseEntity.ok().build();
    }
}