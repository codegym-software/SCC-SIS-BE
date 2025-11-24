package com.example.sis.util;

import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.models.Permission;
import com.example.sis.models.UserRole;
import com.example.sis.models.RolePermission;
import com.example.sis.models.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for creating test data objects
 */
public class TestDataBuilder {
    
    // ==================== ROLE BUILDERS ====================
    
    public static Role buildRole() {
        return buildRole(99, "TEST_ROLE", "Test Role");
    }
    
    public static Role buildRole(Integer id, String code, String name) {
        Role role = new Role();
        role.setRoleId(id);
        role.setCode(code);
        role.setName(name);
        role.setActive(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }
    
    public static CreateRoleRequest buildCreateRoleRequest() {
        return buildCreateRoleRequest("NEW_TEST_ROLE", "New Test Role");
    }
    
    public static CreateRoleRequest buildCreateRoleRequest(String code, String name) {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode(code);
        request.setName(name);
        request.setActive(true);
        request.setPermissionIds(new java.util.HashSet<>());
        return request;
    }
    
    public static UpdateRoleRequest buildUpdateRoleRequest() {
        return buildUpdateRoleRequest("UPDATED_ROLE", "Updated Role");
    }
    
    public static UpdateRoleRequest buildUpdateRoleRequest(String code, String name) {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setCode(code);
        request.setName(name);
        request.setActive(true);
        return request;
    }
    
    // ==================== PERMISSION BUILDERS ====================
    
    public static Permission buildPermission() {
        return buildPermission(99, "TEST_PERMISSION", "Test Permission", "TEST");
    }
    
    public static Permission buildPermission(Integer id, String code, String name, String category) {
        Permission permission = new Permission();
        permission.setPermissionId(id);
        permission.setCode(code);
        permission.setName(name);
        permission.setCategory(category);
        permission.setActive(true);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        return permission;
    }
    
    public static PermissionResponse buildPermissionResponse() {
        return buildPermissionResponse(99, "TEST_PERMISSION", "Test Permission", "TEST");
    }
    
    public static PermissionResponse buildPermissionResponse(Integer id, String code, String name, String category) {
        PermissionResponse response = new PermissionResponse();
        response.setPermissionId(id);
        response.setCode(code);
        response.setName(name);
        response.setCategory(category);
        response.setActive(true);
        return response;
    }
    
    // ==================== USER-ROLE BUILDERS ====================
    
    public static UserRoleRequest buildUserRoleRequest() {
        return buildUserRoleRequest(TestConstants.TEST_USER_REGULAR, TestConstants.TEST_TEACHER_ROLE_ID, null);
    }
    
    public static UserRoleRequest buildUserRoleRequest(Integer userId, Integer roleId, Integer centerId) {
        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);
        request.setCenterId(centerId);
        return request;
    }
    
    public static UserRole buildUserRole() {
        return buildUserRole(99, TestConstants.TEST_USER_REGULAR, TestConstants.TEST_TEACHER_ROLE_ID, null);
    }
    
    public static UserRole buildUserRole(Integer id, Integer userId, Integer roleId, Integer centerId) {
        UserRole userRole = new UserRole();
        userRole.setUserRoleId(id);
        
        User user = new User();
        user.setUserId(userId);
        user.setFullName("Test User " + userId);
        user.setEmail("testuser" + userId + "@test.com");
        user.setKeycloakUserId("keycloak-" + userId);
        userRole.setUser(user);
        
        Role role = buildRole(roleId, "TEST_ROLE_" + roleId, "Test Role " + roleId);
        userRole.setRole(role);
        
        // Center will be set separately if needed
        userRole.setAssignedAt(LocalDateTime.now());
        userRole.setAssignedBy(TestConstants.TEST_ASSIGNED_BY);
        userRole.setRevokedAt(null);
        
        return userRole;
    }
    
    // ==================== ROLE-PERMISSION BUILDERS ====================
    
    public static RolePermission buildRolePermission() {
        return buildRolePermission(99, TestConstants.TEST_TEACHER_ROLE_ID, TestConstants.TEST_PERMISSION_READ_STUDENT);
    }
    
    public static RolePermission buildRolePermission(Integer id, Integer roleId, Integer permissionId) {
        RolePermission rp = new RolePermission();
        rp.setRolePermissionId(id);
        
        Role role = buildRole(roleId, "TEST_ROLE_" + roleId, "Test Role");
        rp.setRole(role);
        
        Permission permission = buildPermission(permissionId, "TEST_PERM_" + permissionId, "Test Permission", "TEST");
        rp.setPermission(permission);
        
        rp.setGrantedAt(LocalDateTime.now());
        rp.setGrantedBy(TestConstants.TEST_ASSIGNED_BY);
        
        return rp;
    }
    
    // ==================== LIST BUILDERS ====================
    
    public static List<Role> buildRoleList(int count) {
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            roles.add(buildRole(i + 1, "ROLE_" + i, "Role " + i));
        }
        return roles;
    }
    
    public static List<Permission> buildPermissionList(int count) {
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            permissions.add(buildPermission(i + 1, "PERM_" + i, "Permission " + i, "CATEGORY_" + (i % 3)));
        }
        return permissions;
    }
    
    public static List<UserRole> buildUserRoleList(int count) {
        List<UserRole> userRoles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            userRoles.add(buildUserRole(i + 1, i + 10, i + 20, null));
        }
        return userRoles;
    }
}
