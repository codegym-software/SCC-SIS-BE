package com.example.sis.services;

import org.springframework.security.core.Authentication;

/**
 * Service for looking up user information from authentication tokens
 */
public interface UserLookupService {

    /**
     * Resolves user ID from authentication token.
     * Priority: email claim → findIdByEmail
     * Fallback: preferred_username claim → findIdByUsername
     * If sub/external_id available, map directly if possible
     *
     * @param authentication The authentication object containing token claims
     * @return User ID if found, null otherwise
     */
    Long resolveUserIdFromToken(Authentication authentication);
}