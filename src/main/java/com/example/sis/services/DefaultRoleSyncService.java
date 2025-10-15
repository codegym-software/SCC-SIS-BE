package com.example.sis.services;

import com.example.sis.enums.RoleScope;

/**
 * Service for auto-assigning default roles to users
 */
public interface DefaultRoleSyncService {

    /**
     * Ensures that a user has their default role assigned if they don't already have role assignments.
     * This method is idempotent and follows the business rules:
     * - If user already has role assignments → return
     * - Get defaultRoleId (+ defaultCenterId) from users table
     * - role = findActiveById(defaultRoleId).orElse(null) → null thì return
     * - scope=GLOBAL → assignIfNotExists(userId, roleId, GLOBAL, null)
     * - scope=CENTER → nếu defaultCenterId!=null → assignIfNotExists(userId, roleId, CENTER, defaultCenterId)
     *
     * @param userId The user ID to check and assign default role for
     */
    void ensureDefaultRoleAssigned(Long userId);
}