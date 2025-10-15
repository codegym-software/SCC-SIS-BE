package com.example.sis.utils;

import com.example.sis.exceptions.BadRequestException;
import com.example.sis.models.Role;
import com.example.sis.enums.RoleScope;

/**
 * Utility class for validating default role and center assignments
 */
public final class DefaultRoleValidationUtil {

    private DefaultRoleValidationUtil() {}

    /**
     * Validates if a role can be assigned as default role
     * @param role The role to validate
     * @param centerId The center ID (can be null for GLOBAL roles)
     * @throws BadRequestException if validation fails
     */
    public static void validateDefaultRoleAssignment(Role role, Integer centerId) {
        // Check if role is active
        if (!role.isActive()) {
            throw new BadRequestException("Cannot assign inactive role as default: " + role.getName());
        }

        // Check if center is required but not provided
        RoleScope scope = RoleScopeUtil.resolveScope(role.getCode());
        if (RoleScope.CENTER.equals(scope) && centerId == null) {
            throw new BadRequestException(
                String.format("Role '%s' requires a center assignment but no center ID provided", role.getName())
            );
        }
    }

    /**
     * Validates default role and center IDs for a user
     * @param defaultRoleId The default role ID
     * @param defaultCenterId The default center ID
     * @throws BadRequestException if validation fails
     */
    public static void validateDefaultRoleFields(Integer defaultRoleId, Integer defaultCenterId) {
        if (defaultRoleId == null) {
            return; // null is allowed
        }

        // Additional validation can be added here if needed
        // For example, checking if role exists and is active
    }
}