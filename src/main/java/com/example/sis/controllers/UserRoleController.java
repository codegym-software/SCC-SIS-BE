package com.example.sis.controllers;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller quản lý user roles
 * 
 * Endpoints:
 * - POST /api/user-roles/assign - Gán role cho user tại center
 * - DELETE /api/user-roles/{userRoleId}/revoke - Thu hồi role của user
 * - GET /api/user-roles/center/{centerId} - Xem user roles tại center
 * - GET /api/user-roles/user/{userId} - Xem roles của user
 * 
 * Security:
 * - Tất cả endpoints yêu cầu SUPER_ADMIN role
 * - Authentication thông qua JWT
 */
@RestController
@RequestMapping("/api/user-roles")
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    /**
     * Gán role cho user tại center cụ thể
     * 
     * @param request thông tin gán role (userId, roleId, centerId)
     * @param jwt     JWT token để lấy thông tin người thực hiện
     * @return thông tin UserRole vừa được gán
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserRoleResponse> assignRoleToUser(
            @RequestBody UserRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String assignedBy = jwt.getSubject(); // Keycloak user ID
        UserRoleResponse response = userRoleService.assignRoleToUser(request, assignedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Thu hồi role của user
     * 
     * @param userRoleId ID của user role cần thu hồi
     * @param jwt        JWT token để lấy thông tin người thực hiện
     * @return 200 OK nếu thành công
     */
    @DeleteMapping("/{userRoleId}/revoke")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> revokeRoleFromUser(
            @PathVariable Integer userRoleId,
            @AuthenticationPrincipal Jwt jwt) {

        String revokedBy = jwt.getSubject(); // Keycloak user ID
        userRoleService.revokeRoleFromUser(userRoleId, revokedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * Xem tất cả user roles tại center cụ thể
     * 
     * @param centerId ID của center
     * @return danh sách UserRole tại center
     */
    @GetMapping("/center/{centerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserRoleResponse>> getUserRolesByCenterId(
            @PathVariable Integer centerId) {

        List<UserRoleResponse> userRoles = userRoleService.getUserRolesByCenterId(centerId);
        return ResponseEntity.ok(userRoles);
    }

    /**
     * Xem tất cả roles của user cụ thể
     * 
     * @param userId ID của user
     * @return danh sách roles của user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserRoleResponse>> getUserRolesByUserId(
            @PathVariable Integer userId) {

        List<UserRoleResponse> userRoles = userRoleService.getUserRolesByUserId(userId);
        return ResponseEntity.ok(userRoles);
    }
}