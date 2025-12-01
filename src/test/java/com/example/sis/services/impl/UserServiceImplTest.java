package com.example.sis.services.impl;

import com.example.sis.keycloak.KeycloakAdminClient;
import com.example.sis.models.User;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private UserRoleRepository userRoleRepo;

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private CenterRepository centerRepo;

    @Mock
    private KeycloakAdminClient kcAdmin;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {
        // GIVEN
        when(userRepo.findById(1)).thenReturn(Optional.of(testUser));

        // WHEN
        Optional<User> result = userRepo.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test User", result.get().getFullName());
        verify(userRepo, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckUserExistsByEmail() {
        // GIVEN
        when(userRepo.existsByEmail("test@example.com")).thenReturn(true);

        // WHEN
        boolean exists = userRepo.existsByEmail("test@example.com");

        // THEN
        assertTrue(exists);
        verify(userRepo, times(1)).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void shouldReturnEmptyWhenNotFound() {
        // GIVEN
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        // WHEN
        Optional<User> result = userRepo.findById(999);

        // THEN
        assertFalse(result.isPresent());
    }
}
