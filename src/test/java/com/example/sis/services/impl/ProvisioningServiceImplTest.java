package com.example.sis.services.impl;

import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProvisioningService Unit Tests")
class ProvisioningServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProvisioningServiceImpl provisioningService;

    private Jwt mockJwt;
    private Authentication mockAuth;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockAuth = mock(Authentication.class);
        mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "keycloak-id-123")
                .claim("email", "test@example.com")
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .claim("preferred_username", "testuser")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("test@example.com");
        testUser.setFullName("User Test");
        testUser.setKeycloakUserId("keycloak-id-123");
    }

    @Test
    @DisplayName("Should return existing user id when user exists")
    void shouldReturnExistingUserId() {
        // GIVEN
        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(userRepository.findIdByKeycloakUserId("keycloak-id-123")).thenReturn(Optional.of(1));

        // WHEN
        Integer result = provisioningService.ensureUserExists(mockAuth);

        // THEN
        assertNotNull(result);
        assertEquals(1, result);
        verify(userRepository, times(1)).findIdByKeycloakUserId("keycloak-id-123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should create new user when user not exists")
    void shouldCreateNewUser() {
        // GIVEN
        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(userRepository.findIdByKeycloakUserId("keycloak-id-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // WHEN
        Integer result = provisioningService.ensureUserExists(mockAuth);

        // THEN
        assertNotNull(result);
        assertEquals(1, result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update existing user with keycloak id")
    void shouldUpdateExistingUserWithKeycloakId() {
        // GIVEN
        User existingUser = new User();
        existingUser.setUserId(2);
        existingUser.setEmail("test@example.com");
        existingUser.setKeycloakUserId(null);

        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(userRepository.findIdByKeycloakUserId("keycloak-id-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // WHEN
        Integer result = provisioningService.ensureUserExists(mockAuth);

        // THEN
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should return null when sub claim is missing")
    void shouldReturnNullWhenSubClaimMissing() {
        // GIVEN
        Jwt jwtWithoutSub = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("email", "test@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(mockAuth.getPrincipal()).thenReturn(jwtWithoutSub);

        // WHEN
        Integer result = provisioningService.ensureUserExists(mockAuth);

        // THEN
        assertNull(result);
        verify(userRepository, never()).save(any(User.class));
    }
}
