package com.example.sis.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom RequestPostProcessor to inject JWT authentication into MockMvc requests
 * This allows integration tests to work with OAuth2 JWT authentication
 */
public class JwtRequestPostProcessor implements RequestPostProcessor {

    private final Jwt jwt;

    private JwtRequestPostProcessor(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    public org.springframework.mock.web.MockHttpServletRequest postProcessRequest(
            org.springframework.mock.web.MockHttpServletRequest request) {
        
        // Extract roles from JWT claims
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            roles = Collections.emptyList();
        }

        // Convert roles to GrantedAuthority objects
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Create JwtAuthenticationToken
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

        // Use Spring Security's authentication request post processor
        return SecurityMockMvcRequestPostProcessors
                .authentication(authentication)
                .postProcessRequest(request);
    }

    /**
     * Create a JWT token with Super Admin role
     */
    public static JwtRequestPostProcessor jwt() {
        return jwtWithRoles("SUPER_ADMIN");
    }

    /**
     * Create a JWT token with specified roles
     */
    public static JwtRequestPostProcessor jwtWithRoles(String... roles) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-user");
        claims.put("preferred_username", "testuser");
        claims.put("email", "testuser@test.com");
        claims.put("roles", Arrays.asList(roles));
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        return new JwtRequestPostProcessor(jwt);
    }

    /**
     * Create a JWT token for Teacher role
     */
    public static JwtRequestPostProcessor jwtTeacher() {
        return jwtWithUser("test-teacher", "mockteacher", "TEACHER");
    }

    /**
     * Create a JWT token for Center Admin role
     */
    public static JwtRequestPostProcessor jwtCenterAdmin() {
        return jwtWithUser("test-centeradmin", "mockcenteradmin", "CENTER_ADMIN");
    }

    /**
     * Create a JWT token for Student role
     */
    public static JwtRequestPostProcessor jwtStudent() {
        return jwtWithUser("test-student", "mockstudent", "STUDENT");
    }

    /**
     * Create a JWT token with custom user ID and roles
     */
    public static JwtRequestPostProcessor jwtWithUser(String userId, String username, String... roles) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("preferred_username", username);
        claims.put("email", username + "@test.com");
        claims.put("roles", Arrays.asList(roles));
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                claims
        );

        return new JwtRequestPostProcessor(jwt);
    }
}
