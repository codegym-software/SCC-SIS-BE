package com.example.sis.controller;

import com.example.sis.model.Permission;
import com.example.sis.model.Role;
import com.example.sis.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Lấy tất cả roles + permissions
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRolesWithPermissions());
    }

    // Lấy permissions của 1 role
    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<Set<Permission>> getPermissionsByRole(@PathVariable Integer roleId) {
        return ResponseEntity.ok(roleService.getPermissionsByRole(roleId));
    }

    // Gán quyền cho 1 role
    @PostMapping("/{roleId}/permissions")
    public ResponseEntity<Role> assignPermissionsToRole(
            @PathVariable Integer roleId,
            @RequestBody List<Integer> permissionIds
    ) {
        return ResponseEntity.ok(roleService.assignPermissionsToRole(roleId, permissionIds));
    }

    // Gỡ quyền khỏi role
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Role> removePermissionFromRole(
            @PathVariable Integer roleId,
            @PathVariable Integer permissionId
    ) {
        return ResponseEntity.ok(roleService.removePermissionFromRole(roleId, permissionId));
    }
}
