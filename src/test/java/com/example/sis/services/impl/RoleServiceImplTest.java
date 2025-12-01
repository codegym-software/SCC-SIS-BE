package com.example.sis.services.impl;

import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.securities.AuthzService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoleServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuthzService authzService;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setRoleId(1);
        testRole.setCode("ADMIN");
        testRole.setName("Administrator");
        testRole.setActive(true);
        testRole.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create role successfully")
    void shouldCreateRole() {
        // GIVEN
        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode("NEW_ROLE");
        request.setName("New Role");
        request.setPermissionIds(new HashSet<>());

        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // WHEN
        RoleResponse result = roleService.createRole(request);

        // THEN
        assertNotNull(result);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should get role by ID")
    void shouldGetRoleById() {
        // GIVEN
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));

        // WHEN
        RoleResponse result = roleService.getRoleById(1);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getRoleId());
        verify(roleRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should update role")
    void shouldUpdateRole() {
        // GIVEN
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");
        request.setPermissionIds(new ArrayList<>());

        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // WHEN
        RoleResponse result = roleService.updateRole(1, request);

        // THEN
        assertNotNull(result);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should delete role")
    void shouldDeleteRole() {
        // GIVEN
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));

        // WHEN
        roleService.deleteRole(1);

        // THEN
        verify(roleRepository, times(1)).findById(1);
    }
}
