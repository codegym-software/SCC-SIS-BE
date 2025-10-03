package com.example.sis.controllers;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;
import com.example.sis.services.RolePermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Role ↔ Permission management (Super Admin only).
 * Base path: /api/roles/{roleId}/permissions
 */
@RestController
@RequestMapping("/api/roles/{roleId}/permissions")
@PreAuthorize("@authz.isSuperAdmin(authentication)")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /** GET — list assigned permissions (filtered & paged). Ordered by name asc. */
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> listAssigned(
            @PathVariable Integer roleId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) Integer size
    ) {
        return ResponseEntity.ok(
                rolePermissionService.listAssigned(roleId, q, category, page, size, null)
        );
    }

    /** GET /unassigned — list unassigned permissions (filtered & paged). Ordered by name asc. */
    @GetMapping("/unassigned")
    public ResponseEntity<List<PermissionResponse>> listUnassigned(
            @PathVariable Integer roleId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) Integer size
    ) {
        return ResponseEntity.ok(
                rolePermissionService.listUnassigned(roleId, q, category, page, size, null)
        );
    }

    /**
     * POST /{permissionId} — assign ONE (idempotent).
     * Returns 201 with mapping payload (existing mapping is returned as-is).
     */
    @PostMapping("/{permissionId}")
    public ResponseEntity<RolePermissionResponse> assignOne(
            @PathVariable Integer roleId,
            @PathVariable Integer permissionId,
            Authentication authentication
    ) {
        String grantedBy = ((Jwt) authentication.getPrincipal()).getSubject();

        RolePermissionRequest req = new RolePermissionRequest();
        req.setRoleId(roleId);
        req.setPermissionId(permissionId);

        RolePermissionResponse res = rolePermissionService.assignPermissionToRole(req, grantedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** POST — assign MANY (idempotent per item). Returns created mappings only. */
    @PostMapping
    public ResponseEntity<List<RolePermissionResponse>> assignMany(
            @PathVariable Integer roleId,
            @Valid @RequestBody List<Integer> permissionIds,
            Authentication authentication
    ) {
        String grantedBy = ((Jwt) authentication.getPrincipal()).getSubject();
        List<RolePermissionResponse> result =
                rolePermissionService.assignMultiplePermissionsToRole(roleId, permissionIds, grantedBy);
        return ResponseEntity.ok(result);
    }

    /** DELETE /{permissionId} — revoke ONE. Returns 204. */
    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> revokeOne(
            @PathVariable Integer roleId,
            @PathVariable Integer permissionId
    ) {
        rolePermissionService.revokePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    /** DELETE — revoke MANY (body = [permissionId]). Returns 204. */
    @DeleteMapping
    public ResponseEntity<Void> revokeMany(
            @PathVariable Integer roleId,
            @RequestBody List<Integer> permissionIds
    ) {
        rolePermissionService.revokeMultiplePermissionsFromRole(roleId, permissionIds);
        return ResponseEntity.noContent().build();
    }
}
