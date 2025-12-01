package com.example.sis.services.impl;

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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLookupService Unit Tests")
class UserLookupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupServiceImpl userLookupService;

    private Jwt mockJwt;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        mockAuth = mock(Authentication.class);
        mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("email", "test@example.com")
                .claim("preferred_username", "testuser")
                .claim("sub", "keycloak-id-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("Should resolve user ID from email claim")
    void shouldResolveUserIdFromEmail() {
        // GIVEN
        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(userRepository.findIdByEmail("test@example.com")).thenReturn(Optional.of(1L));

        // WHEN
        Long result = userLookupService.resolveUserIdFromToken(mockAuth);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result);
        verify(userRepository, times(1)).findIdByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should fallback to preferred_username when email not found")
    void shouldFallbackToPreferredUsername() {
        // GIVEN
        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(userRepository.findIdByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findIdByUsername("testuser")).thenReturn(Optional.of(2L));

        // WHEN
        Long result = userLookupService.resolveUserIdFromToken(mockAuth);

        // THEN
        assertNotNull(result);
        assertEquals(2L, result);
        verify(userRepository, times(1)).findIdByUsername("testuser");
    }

    @Test
    @DisplayName("Should return null when authentication is null")
    void shouldReturnNullWhenAuthenticationIsNull() {
        // WHEN
        Long result = userLookupService.resolveUserIdFromToken(null);

        // THEN
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when no user found")
    void shouldReturnNullWhenNoUserFound() {
        // GIVEN
        when(mockAuth.getPrincipal()).thenReturn(mockJwt);
        when(userRepository.findIdByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findIdByUsername(anyString())).thenReturn(Optional.empty());

        // WHEN
        Long result = userLookupService.resolveUserIdFromToken(mockAuth);

        // THEN
        assertNull(result);
    }
}
