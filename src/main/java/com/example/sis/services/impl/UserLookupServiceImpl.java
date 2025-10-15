package com.example.sis.services.impl;

import com.example.sis.repositories.UserRepository;
import com.example.sis.services.UserLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of UserLookupService for resolving user ID from JWT tokens
 */
@Service
public class UserLookupServiceImpl implements UserLookupService {

    private static final Logger logger = LoggerFactory.getLogger(UserLookupServiceImpl.class);

    private final UserRepository userRepository;

    public UserLookupServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves user ID from authentication token.
     * Priority: email claim → findIdByEmail
     * Fallback: preferred_username claim → findIdByUsername
     * If sub/external_id available, map directly if possible
     *
     * @param authentication The authentication object containing token claims
     * @return User ID if found, null otherwise
     */
    @Override
    public Long resolveUserIdFromToken(Authentication authentication) {
        if (authentication == null) {
            logger.debug("Authentication is null");
            return null;
        }

        try {
            // Try to get JWT token from authentication
            Jwt jwt = extractJwtFromAuthentication(authentication);
            if (jwt == null) {
                logger.debug("No JWT token found in authentication");
                return null;
            }

            // Priority 1: Try email claim
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.trim().isEmpty()) {
                logger.debug("Looking up user by email: {}", email);
                Optional<Long> userIdOpt = userRepository.findIdByEmail(email);
                if (userIdOpt.isPresent()) {
                    logger.debug("Found user ID {} for email {}", userIdOpt.get(), email);
                    return userIdOpt.get();
                }
            }

            // Fallback: Try preferred_username claim
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.trim().isEmpty()) {
                logger.debug("Looking up user by preferred_username: {}", preferredUsername);
                Optional<Long> userIdOpt = userRepository.findIdByUsername(preferredUsername);
                if (userIdOpt.isPresent()) {
                    logger.debug("Found user ID {} for preferred_username {}", userIdOpt.get(), preferredUsername);
                    return userIdOpt.get();
                }
            }

            // Additional fallback: Try sub claim (Keycloak user ID)
            String sub = jwt.getClaimAsString("sub");
            if (sub != null && !sub.trim().isEmpty()) {
                logger.debug("Looking up user by sub (Keycloak ID): {}", sub);
                // Note: This assumes the sub claim maps to keycloak_user_id in users table
                // You might need to add a findIdByKeycloakUserId method to UserRepository
                // For now, we'll skip this as it requires additional repository method
            }

            logger.warn("Could not resolve user ID from token claims");
            return null;

        } catch (Exception ex) {
            logger.error("Error resolving user ID from token: {}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Extracts JWT token from authentication object
     * @param authentication The authentication object
     * @return JWT token if available, null otherwise
     */
    private Jwt extractJwtFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            return (Jwt) principal;
        }

        // Try to get credentials if it's a JWT string
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String) {
            // This is a fallback - in practice, Spring Security should provide JWT as principal
            logger.debug("JWT token found as credentials (unusual case)");
        }

        return null;
    }
}