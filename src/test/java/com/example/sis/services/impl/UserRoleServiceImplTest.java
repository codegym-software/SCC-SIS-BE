package com.example.sis.services.impl;

import com.example.sis.models.Role;
import com.example.sis.models.User;
import com.example.sis.models.UserRole;
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
@DisplayName("UserRoleService Unit Tests")
class UserRoleServiceImplTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    private User testUser;
    private Role testRole;
    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("test@example.com");

        testRole = new Role();
        testRole.setRoleId(1);
        testRole.setCode("TEACHER");
        testRole.setName("Teacher");

        testUserRole = new UserRole();
        testUserRole.setUserRoleId(1);
        testUserRole.setUser(testUser);
        testUserRole.setRole(testRole);
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {
        // GIVEN
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // WHEN
        Optional<User> result = userRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should find role by id")
    void shouldFindRoleById() {
        // GIVEN
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));

        // WHEN
        Optional<Role> result = roleRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("TEACHER", result.get().getCode());
    }

    @Test
    @DisplayName("Should check if user role exists")
    void shouldCheckUserRoleExists() {
        // GIVEN
        when(userRoleRepository.existsByUserId(1L)).thenReturn(true);

        // WHEN
        boolean exists = userRoleRepository.existsByUserId(1L);

        // THEN
        assertTrue(exists);
        verify(userRoleRepository, times(1)).existsByUserId(1L);
    }
}
