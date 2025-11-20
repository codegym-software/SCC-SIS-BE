package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.permission.PermissionGroupResponse;
import com.example.sis.models.Permission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.util.TestDataBuilder;

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
 * Unit tests for PermissionServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Unit Tests")
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testPermission = TestDataBuilder.buildPermission(1, "READ_STUDENT", "View Students", "STUDENT");
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("Should search permissions with query")
    void shouldSearchPermissionsWithQuery() {
        // Given
        String query = "student";
        List<Permission> permissions = List.of(testPermission);
        org.springframework.data.domain.Page<Permission> page = 
            new org.springframework.data.domain.PageImpl<>(permissions);
        
        when(permissionRepository.search(anyString(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // When
        List<PermissionResponse> result = permissionService.search(query, null, true, 0, 10, "name,asc");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(permissionRepository, times(1)).search(anyString(), any(), any(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should search permissions by category")
    void shouldSearchPermissionsByCategory() {
        // Given
        String category = "STUDENT";
        List<Permission> permissions = List.of(testPermission);
        org.springframework.data.domain.Page<Permission> page = 
            new org.springframework.data.domain.PageImpl<>(permissions);
        
        when(permissionRepository.search(any(), anyString(), any(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // When
        List<PermissionResponse> result = permissionService.search(null, category, true, 0, 10, "name,asc");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Should get permission by ID successfully")
    void shouldGetPermissionByIdSuccessfully() {
        // Given
        when(permissionRepository.findById(1)).thenReturn(Optional.of(testPermission));

        // When
        PermissionResponse result = permissionService.getById(1);

        // Then
        assertNotNull(result);
        assertEquals(testPermission.getPermissionId(), result.getPermissionId());
        assertEquals(testPermission.getCode(), result.getCode());
        assertEquals(testPermission.getName(), result.getName());
        assertEquals(testPermission.getCategory(), result.getCategory());
        
        verify(permissionRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should throw exception when permission ID not found")
    void shouldThrowExceptionWhenPermissionNotFound() {
        // Given
        when(permissionRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.getById(999);
        });

        verify(permissionRepository, times(1)).findById(999);
    }

    // ==================== LIST CATEGORIES TESTS ====================

    @Test
    @DisplayName("Should list all categories")
    void shouldListAllCategories() {
        // Given - use actual method from repository
        List<String> categories = List.of("STUDENT", "TEACHER", "ROLE");
        when(permissionRepository.findDistinctCategoriesByActiveTrue()).thenReturn(categories);

        // When
        List<String> result = permissionService.listCategories();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("STUDENT"));
        verify(permissionRepository, times(1)).findDistinctCategoriesByActiveTrue();
    }

    // ==================== GROUPS TESTS ====================

    @Test
    @DisplayName("Should group permissions by category")
    void shouldGroupPermissionsByCategory() {
        // Given
        List<Permission> permissions = List.of(
            TestDataBuilder.buildPermission(1, "READ_STUDENT", "View Students", "STUDENT"),
            TestDataBuilder.buildPermission(2, "CREATE_STUDENT", "Create Student", "STUDENT"),
            TestDataBuilder.buildPermission(3, "READ_TEACHER", "View Teachers", "TEACHER")
        );
        org.springframework.data.domain.Page<Permission> page = 
            new org.springframework.data.domain.PageImpl<>(permissions);
        
        when(permissionRepository.search(any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // When
        List<PermissionGroupResponse> result = permissionService.groups(null, null, true, null, false);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    @DisplayName("Should filter groups by category")
    void shouldFilterGroupsByCategory() {
        // Given
        String category = "STUDENT";
        List<Permission> permissions = List.of(
            TestDataBuilder.buildPermission(1, "READ_STUDENT", "View Students", "STUDENT"),
            TestDataBuilder.buildPermission(2, "CREATE_STUDENT", "Create Student", "STUDENT")
        );
        org.springframework.data.domain.Page<Permission> page = 
            new org.springframework.data.domain.PageImpl<>(permissions);
        
        when(permissionRepository.search(any(), anyString(), any(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // When
        List<PermissionGroupResponse> result = permissionService.groups(null, category, true, null, false);

        // Then
        assertNotNull(result);
        result.forEach(group -> 
            assertTrue(group.getCategory().equals(category) || group.getItems().isEmpty())
        );
    }

    @Test
    @DisplayName("Should search permissions in groups")
    void shouldSearchPermissionsInGroups() {
        // Given
        String query = "student";
        List<Permission> permissions = List.of(
            TestDataBuilder.buildPermission(1, "READ_STUDENT", "View Students", "STUDENT"),
            TestDataBuilder.buildPermission(2, "CREATE_STUDENT", "Create Student", "STUDENT")
        );
        org.springframework.data.domain.Page<Permission> page = 
            new org.springframework.data.domain.PageImpl<>(permissions);
        
        when(permissionRepository.search(anyString(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // When
        List<PermissionGroupResponse> result = permissionService.groups(query, null, true, null, false);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
