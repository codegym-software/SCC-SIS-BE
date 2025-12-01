package com.example.sis.services.impl;

import com.example.sis.configs.AuthProps;
import com.example.sis.models.User;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultRoleSyncService Unit Tests")
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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should skip when auto assign disabled")
    void shouldSkipWhenAutoAssignDisabled() {
        // GIVEN
        when(authProps.isAutoAssignEnabled()).thenReturn(false);

        // WHEN
        defaultRoleSyncService.ensureDefaultRoleAssigned(1L);

        // THEN
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should skip when user already has roles")
    void shouldSkipWhenUserHasRoles() {
        // GIVEN
        when(authProps.isAutoAssignEnabled()).thenReturn(true);
        when(userRoleRepository.existsByUserId(1L)).thenReturn(true);

        // WHEN
        defaultRoleSyncService.ensureDefaultRoleAssigned(1L);

        // THEN
        verify(userRoleService, never()).assignRoleToUser(any(), any());
    }

    @Test
    @DisplayName("Should skip when userId is null")
    void shouldSkipWhenUserIdNull() {
        // WHEN
        defaultRoleSyncService.ensureDefaultRoleAssigned(null);

        // THEN
        verify(authProps, never()).isAutoAssignEnabled();
    }
}
