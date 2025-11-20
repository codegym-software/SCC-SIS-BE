package com.example.sis.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;

/**
 * Utility class for creating mock security contexts and JWT tokens for testing
 */
public class SecurityTestUtils {
    
    /**
     * Create a mock JWT with default claims
     */
    public static Jwt createMockJwt() {
        return createMockJwt(TestConstants.TEST_JWT_SUBJECT, List.of("SUPER_ADMIN"));
    }
    
    /**
     * Create a mock JWT with custom subject and roles
     */
    public static Jwt createMockJwt(String subject, List<String> roles) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "HS256");
        headers.put("typ", "JWT");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
        claims.put("roles", roles);
        claims.put("preferred_username", subject);
        
        return new Jwt(
            "mock-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            headers,
            claims
        );
    }
    
    /**
     * Create a JwtAuthenticationToken with default authorities
     */
    public static Authentication createMockAuthentication() {
        return createMockAuthentication(TestConstants.TEST_JWT_SUBJECT, List.of("SUPER_ADMIN"));
    }
    
    /**
     * Create a JwtAuthenticationToken with custom subject and roles
     */
    public static Authentication createMockAuthentication(String subject, List<String> roles) {
        Jwt jwt = createMockJwt(subject, roles);
        
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        
        return new JwtAuthenticationToken(jwt, authorities);
    }
    
    /**
     * Create a mock JWT for Super Admin
     */
    public static Jwt createSuperAdminJwt() {
        return createMockJwt(TestConstants.TEST_USERNAME_SUPER_ADMIN, List.of("SUPER_ADMIN"));
    }
    
    /**
     * Create a mock JWT for Center Admin
     */
    public static Jwt createCenterAdminJwt() {
        return createMockJwt(TestConstants.TEST_USERNAME_CENTER_ADMIN, List.of("CENTER_ADMIN"));
    }
    
    /**
     * Create a mock JWT for Teacher
     */
    public static Jwt createTeacherJwt() {
        return createMockJwt(TestConstants.TEST_USERNAME_TEACHER, List.of("TEACHER"));
    }
    
    /**
     * Create a mock JWT for Student
     */
    public static Jwt createStudentJwt() {
        return createMockJwt(TestConstants.TEST_USERNAME_STUDENT, List.of("STUDENT"));
    }
    
    /**
     * Create authentication for Super Admin
     */
    public static Authentication createSuperAdminAuthentication() {
        return createMockAuthentication(TestConstants.TEST_USERNAME_SUPER_ADMIN, List.of("SUPER_ADMIN"));
    }
    
    /**
     * Create authentication for Center Admin
     */
    public static Authentication createCenterAdminAuthentication() {
        return createMockAuthentication(TestConstants.TEST_USERNAME_CENTER_ADMIN, List.of("CENTER_ADMIN"));
    }
    
    /**
     * Create authentication for Teacher
     */
    public static Authentication createTeacherAuthentication() {
        return createMockAuthentication(TestConstants.TEST_USERNAME_TEACHER, List.of("TEACHER"));
    }
    
    /**
     * Create authentication for Student
     */
    public static Authentication createStudentAuthentication() {
        return createMockAuthentication(TestConstants.TEST_USERNAME_STUDENT, List.of("STUDENT"));
    }
    
    private SecurityTestUtils() {
        // Utility class
    }
}
