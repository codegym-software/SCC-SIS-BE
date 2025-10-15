package com.example.sis.services;

import org.springframework.security.core.Authentication;

/**
 * Service for provisioning users from JWT tokens when they don't exist in database
 */
public interface ProvisioningService {

    /**
     * Ensures that a user exists in the database based on JWT token claims.
     * If user doesn't exist, creates a new user record with information from JWT token.
     *
     * @param authentication The authentication object containing JWT token
     * @return User ID if successful, null if sub claim is missing or provisioning fails
     */
    Integer ensureUserExists(Authentication authentication);
}