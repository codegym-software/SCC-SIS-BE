package com.example.sis.controllers;

import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.services.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

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
     *  - active = null hoặc không truyền -> mặc định trả các role đang active
     *  - active = false -> trả tất cả role
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles(
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok(roleService.listRoles(active));
    }
}
