package com.example.sis.controllers;

import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.services.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("@authz.isSuperAdmin(authentication)") // Super Admin only for all role management
public class RoleController {

    private final RoleService roleService;
    public RoleController(RoleService roleService) { this.roleService = roleService; }

    /**
     * GET /api/roles?active=true|false
     * - active = null or true  -> return only active roles (default)
     * - active = false         -> return ALL roles
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles(
            @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(roleService.listRoles(active));
    }

    /**
     * GET /api/roles/{id}
     * Fetch role by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Integer id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    /**
     * POST /api/roles
     * Create a new role.
     * 201 Created on success.
     */
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse created = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/roles/{id}
     * Update a role.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    /**
     * DELETE /api/roles/{id}
     * Soft delete a role.
     * 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
