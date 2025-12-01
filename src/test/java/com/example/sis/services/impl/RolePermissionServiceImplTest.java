package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;
import com.example.sis.models.Permission;
import com.example.sis.models.Role;
import com.example.sis.models.RolePermission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.repositories.RoleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolePermissionService Unit Tests")
class RolePermissionServiceImplTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    private Role testRole;
    private Permission testPermission;
    private RolePermission testRolePermission;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setRoleId(1);
        testRole.setCode("TEACHER");
        testRole.setName("Teacher");

        testPermission = new Permission();
        testPermission.setPermissionId(1);
        testPermission.setCode("READ_STUDENT");
        testPermission.setName("Read Student");

        testRolePermission = new RolePermission();
        testRolePermission.setRole(testRole);
        testRolePermission.setPermission(testPermission);
    }

    @Test
    @DisplayName("Should list assigned permissions")
    void shouldListAssignedPermissions() {
        // GIVEN
        Page<Permission> permissionPage = new PageImpl<>(Arrays.asList(testPermission));
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(rolePermissionRepository.pageAssignedPermissions(anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(permissionPage);

        // WHEN
        List<PermissionResponse> result = rolePermissionService.listAssigned(1, null, null, 0, 20, null);

        // THEN
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(rolePermissionRepository, times(1)).pageAssignedPermissions(anyInt(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should list unassigned permissions")
    void shouldListUnassignedPermissions() {
        // GIVEN
        Page<Permission> permissionPage = new PageImpl<>(Arrays.asList(testPermission));
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(rolePermissionRepository.pageUnassignedPermissions(anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(permissionPage);

        // WHEN
        List<PermissionResponse> result = rolePermissionService.listUnassigned(1, null, null, 0, 20, null);

        // THEN
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(rolePermissionRepository, times(1)).pageUnassignedPermissions(anyInt(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should assign permission to role")
    void shouldAssignPermissionToRole() {
        // GIVEN
        RolePermissionRequest request = new RolePermissionRequest();
        request.setRoleId(1);
        request.setPermissionId(1);

        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById(1)).thenReturn(Optional.of(testPermission));
        when(rolePermissionRepository.findByRoleAndPermission(testRole, testPermission))
                .thenReturn(Optional.empty());
        when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(testRolePermission);

        // WHEN
        RolePermissionResponse result = rolePermissionService.assignPermissionToRole(request, "admin");

        // THEN
        assertNotNull(result);
        verify(rolePermissionRepository, times(1)).save(any(RolePermission.class));
    }

    @Test
    @DisplayName("Should return existing when assigning duplicate permission")
    void shouldReturnExistingWhenAssigningDuplicate() {
        // GIVEN
        RolePermissionRequest request = new RolePermissionRequest();
        request.setRoleId(1);
        request.setPermissionId(1);

        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById(1)).thenReturn(Optional.of(testPermission));
        when(rolePermissionRepository.findByRoleAndPermission(testRole, testPermission))
                .thenReturn(Optional.of(testRolePermission));

        // WHEN
        RolePermissionResponse result = rolePermissionService.assignPermissionToRole(request, "admin");

        // THEN
        assertNotNull(result);
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void shouldThrowExceptionWhenRoleNotFound() {
        // GIVEN
        RolePermissionRequest request = new RolePermissionRequest();
        request.setRoleId(999);
        request.setPermissionId(1);

        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(Exception.class, () -> rolePermissionService.assignPermissionToRole(request, "admin"));
    }

    @Test
    @DisplayName("Should throw exception when permission not found")
    void shouldThrowExceptionWhenPermissionNotFound() {
        // GIVEN
        RolePermissionRequest request = new RolePermissionRequest();
        request.setRoleId(1);
        request.setPermissionId(999);

        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(Exception.class, () -> rolePermissionService.assignPermissionToRole(request, "admin"));
    }
}
