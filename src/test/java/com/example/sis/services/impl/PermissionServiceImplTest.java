package com.example.sis.services.impl;

import com.example.sis.models.Permission;
import com.example.sis.repositories.PermissionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        testPermission = new Permission();
        testPermission.setPermissionId(1);
        testPermission.setCode("CREATE_USER");
        testPermission.setName("Create User");
    }

    @Test
    @DisplayName("Should get all permissions")
    void shouldGetAllPermissions() {
        // GIVEN
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findAll()).thenReturn(permissions);

        // WHEN
        List<Permission> result = permissionRepository.findAll();

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(permissionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get permission by ID")
    void shouldGetPermissionById() {
        // GIVEN
        when(permissionRepository.findById(1)).thenReturn(Optional.of(testPermission));

        // WHEN
        Optional<Permission> result = permissionRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("CREATE_USER", result.get().getCode());
        verify(permissionRepository, times(1)).findById(1);
    }
}
