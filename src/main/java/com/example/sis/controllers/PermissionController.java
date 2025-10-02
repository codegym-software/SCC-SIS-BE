package com.example.sis.controllers;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.services.PermissionService;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Permissions management (Super Admin only).
 * Unified list endpoint supports filtering & pagination.
 */
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("@authz.isSuperAdmin(authentication)")
public class PermissionController {

    private final PermissionService permissionService;
    public PermissionController(PermissionService permissionService) { this.permissionService = permissionService; }

    /**
     * GET /api/permissions
     * - active: null/true -> only active (default), false -> all
     * - q: fuzzy search on code/name
     * - category: exact match
     * - page/size/sort: pagination (e.g. name,asc)
     */
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

    /** GET /api/permissions/categories — distinct categories (active only). */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(permissionService.listCategories());
    }

    /** GET /api/permissions/{id} — get permission by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(permissionService.getById(id));
    }
}
