package com.example.sis.services.impl;

import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.models.Permission;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.securities.AuthzService;
import com.example.sis.util.TestDataBuilder;
import com.example.sis.util.TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoleServiceImpl
 * Tests business logic in isolation with mocked dependencies
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
    private CreateRoleRequest createRequest;
    private UpdateRoleRequest updateRequest;

    @BeforeEach
    void setUp() {
        testRole = TestDataBuilder.buildRole(1, "TEST_ROLE", "Test Role");
        createRequest = TestDataBuilder.buildCreateRoleRequest("NEW_ROLE", "New Role");
        updateRequest = TestDataBuilder.buildUpdateRoleRequest("UPDATED_ROLE", "Updated Role");
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should create role successfully with valid data")
    void shouldCreateRoleSuccessfully() {
        // Given
        when(roleRepository.existsByCode(createRequest.getCode())).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        RoleResponse result = roleService.createRole(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(testRole.getRoleId(), result.getRoleId());
        assertEquals(testRole.getCode(), result.getCode());
        assertEquals(testRole.getName(), result.getName());
        
        verify(roleRepository, times(1)).existsByCode(createRequest.getCode());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when creating role with duplicate code")
    void shouldThrowExceptionWhenRoleCodeExists() {
        // Given
        when(roleRepository.existsByCode(createRequest.getCode())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roleService.createRole(createRequest);
        });

        assertTrue(exception.getMessage().contains("đã tồn tại"));
        verify(roleRepository, times(1)).existsByCode(createRequest.getCode());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should create role with permissions when permission IDs provided")
    void shouldCreateRoleWithPermissions() {
        // Given
        java.util.Set<Integer> permissionIds = new java.util.HashSet<>(List.of(1, 2, 3));
        createRequest.setPermissionIds(permissionIds);
        
        List<Permission> permissions = TestDataBuilder.buildPermissionList(3);
        
        when(roleRepository.existsByCode(createRequest.getCode())).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);

        // When
        RoleResponse result = roleService.createRole(createRequest);

        // Then
        assertNotNull(result);
        verify(permissionRepository, times(1)).findAllById(permissionIds);
    }

    @Test
    @DisplayName("Should throw exception when some permission IDs do not exist")
    void shouldThrowExceptionWhenPermissionIdsInvalid() {
        // Given
        java.util.Set<Integer> permissionIds = new java.util.HashSet<>(List.of(1, 2, 999));
        createRequest.setPermissionIds(permissionIds);
        
        List<Permission> permissions = TestDataBuilder.buildPermissionList(2); // Only 2 found
        
        when(roleRepository.existsByCode(createRequest.getCode())).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            roleService.createRole(createRequest);
        });

        verify(roleRepository, times(1)).save(any(Role.class));
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    @DisplayName("Should get role by ID successfully when role exists")
    void shouldGetRoleByIdSuccessfully() {
        // Given
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));

        // When
        RoleResponse result = roleService.getRoleById(1);

        // Then
        assertNotNull(result);
        assertEquals(testRole.getRoleId(), result.getRoleId());
        assertEquals(testRole.getCode(), result.getCode());
        assertEquals(testRole.getName(), result.getName());
        
        verify(roleRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should throw exception when role ID not found")
    void shouldThrowExceptionWhenRoleNotFound() {
        // Given
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            roleService.getRoleById(999);
        });

        verify(roleRepository, times(1)).findById(999);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update role successfully")
    void shouldUpdateRoleSuccessfully() {
        // Given
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        RoleResponse result = roleService.updateRole(1, updateRequest);

        // Then
        assertNotNull(result);
        verify(roleRepository, times(1)).findById(1);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when updating with duplicate code")
    void shouldThrowExceptionWhenUpdatingWithDuplicateCode() {
        // Given
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(roleRepository.existsByCode(updateRequest.getCode())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roleService.updateRole(1, updateRequest);
        });

        assertTrue(exception.getMessage().contains("đã tồn tại"));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent role")
    void shouldThrowExceptionWhenUpdatingNonExistentRole() {
        // Given
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            roleService.updateRole(999, updateRequest);
        });

        verify(roleRepository, times(1)).findById(999);
        verify(roleRepository, never()).save(any(Role.class));
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should soft delete role successfully")
    void shouldDeleteRoleSuccessfully() {
        // Given
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        roleService.deleteRole(1);

        // Then
        verify(roleRepository, times(1)).findById(1);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent role")
    void shouldThrowExceptionWhenDeletingNonExistentRole() {
        // Given
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            roleService.deleteRole(999);
        });

        verify(roleRepository, times(1)).findById(999);
        verify(roleRepository, never()).save(any(Role.class));
    }

    // ==================== LIST TESTS ====================

    @Test
    @DisplayName("Should list all active roles")
    void shouldListAllActiveRoles() {
        // Given
        List<Role> roles = TestDataBuilder.buildRoleList(3);
        when(roleRepository.findByActiveTrueOrderByNameAsc(any())).thenReturn(
            new org.springframework.data.domain.PageImpl<>(roles)
        );

        // When
        List<RoleResponse> result = roleService.listRoles(true);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(roleRepository, times(1)).findByActiveTrueOrderByNameAsc(any());
    }

    @Test
    @DisplayName("Should list all roles including inactive when active=false")
    void shouldListAllRolesIncludingInactive() {
        // Given
        List<Role> roles = TestDataBuilder.buildRoleList(5);
        when(roleRepository.findAllByOrderByNameAsc(any())).thenReturn(
            new org.springframework.data.domain.PageImpl<>(roles)
        );

        // When
        List<RoleResponse> result = roleService.listRoles(false);

        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(roleRepository, times(1)).findAllByOrderByNameAsc(any());
    }
}
