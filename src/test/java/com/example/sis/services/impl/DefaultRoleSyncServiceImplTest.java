package com.example.sis.services.impl;

import com.example.sis.configs.AuthProps;
import com.example.sis.enums.RoleScope;
import com.example.sis.models.Role;
import com.example.sis.models.User;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRoleSyncServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private AuthProps authProps;

    @InjectMocks
    private DefaultRoleSyncServiceImpl defaultRoleSyncService;

    private User testUser;
    private Role superAdminRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setDefaultRoleId(null); // null để test auto-assign từ config
        testUser.setDefaultCenterId(null);

        superAdminRole = new Role();
        superAdminRole.setRoleId(1);
        superAdminRole.setCode("SUPER_ADMIN");
        superAdminRole.setName("Super Admin");
        superAdminRole.setActive(true);
    }

    @Test
    void testAutoAssignSuperAdmin_WhenUserHasNoDefaultRole() {
        // Given
        Long userId = 1L;
        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(authProps.getDefaultRoleCode()).thenReturn("SUPER_ADMIN");
        when(roleRepository.findIdByCode("SUPER_ADMIN")).thenReturn(Optional.of(1));
        when(roleRepository.findActiveById(1)).thenReturn(Optional.of(superAdminRole));

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verify(userRoleService).assignIfNotExists(eq(userId), eq(1), eq(RoleScope.GLOBAL), eq(null));
    }

    @Test
    void testSkipAutoAssign_WhenDisabled() {
        // Given
        Long userId = 1L;
        when(authProps.isAutoAssignEnabled()).thenReturn(false);

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verifyNoInteractions(userRepository);
        verifyNoInteractions(userRoleRepository);
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void testSkipAutoAssign_WhenUserAlreadyHasRoles() {
        // Given
        Long userId = 1L;
        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(userId)).thenReturn(true);

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verify(userRoleRepository).existsByUserId(userId);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void testSkipAutoAssign_WhenUserNotFound() {
        // Given
        Long userId = 1L;
        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verify(userRepository).findById(1);
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void testSkipAutoAssign_WhenNoDefaultRoleCodeConfigured() {
        // Given
        Long userId = 1L;
        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(authProps.getDefaultRoleCode()).thenReturn(null);

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verify(authProps).getDefaultRoleCode();
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void testSkipAutoAssign_WhenDefaultRoleNotFound() {
        // Given
        Long userId = 1L;
        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(authProps.getDefaultRoleCode()).thenReturn("SUPER_ADMIN");
        when(roleRepository.findIdByCode("SUPER_ADMIN")).thenReturn(Optional.empty());

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verify(roleRepository).findIdByCode("SUPER_ADMIN");
        verifyNoInteractions(userRoleService);
    }

    @Test
    void testSkipAutoAssign_WhenRoleInactive() {
        // Given
        Long userId = 1L;
        Role inactiveRole = new Role();
        inactiveRole.setRoleId(1);
        inactiveRole.setCode("SUPER_ADMIN");
        inactiveRole.setActive(false);

        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(authProps.getDefaultRoleCode()).thenReturn("SUPER_ADMIN");
        when(roleRepository.findIdByCode("SUPER_ADMIN")).thenReturn(Optional.of(1));
        when(roleRepository.findActiveById(1)).thenReturn(Optional.empty());

        // When
        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);

        // Then
        verify(roleRepository).findActiveById(1);
        verifyNoInteractions(userRoleService);
    }
}