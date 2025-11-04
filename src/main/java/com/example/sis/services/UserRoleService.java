package com.example.sis.services;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.dtos.userrole.UserRoleAssignmentSummaryResponse;

import java.util.List;

/**
 * Manage User ↔ Role assignments (global or per-center).
 *
 * Rules:
 * - GLOBAL role → centerId must be null, only Super Admin can assign/revoke.
 * - CENTER role → centerId required; Super Admin or Center Manager of that center.
 */
public interface UserRoleService {

    /**
     * Assign ONE role to a user (idempotent per (roleId, centerId)).
     * - Validates scope: GLOBAL(centerId=null) vs CENTER(centerId required).
     * - GLOBAL-exclusive rule is enforced in implementation.
     */
    UserRoleResponse assignRoleToUser(UserRoleRequest request, String assignedBy);

    /**
     * Assign MANY roles to a user (idempotent per item).
     * - All items must target the same user.
     * - Validates scope per item; GLOBAL-exclusive rule enforced.
     */
    List<UserRoleResponse> assignRolesToUser(Integer userId, List<UserRoleRequest> requests, String assignedBy);

    /**
     * Assign MANY roles to a user with summary response (idempotent per item).
     * - All items must target the same user.
     * - Validates scope per item with Vietnamese error messages.
     * - Enforces business rules: GLOBAL ≤ 1, CENTER ≤ 3.
     * - Returns summary with createdCount, skippedCount, errors.
     */
    UserRoleAssignmentSummaryResponse assignRolesToUserWithSummary(Integer userId, List<UserRoleRequest> requests, String assignedBy);

    /** Soft revoke ONE user-role by id (no-op if already revoked). */
    void revokeRoleFromUser(Integer userRoleId, String revokedBy);

    /** Soft revoke MANY user-roles by ids (bulk & efficient). */
    void revokeRolesFromUsers(List<Integer> userRoleIds, String revokedBy);

    /** List active user-roles in a center (paged, ordered by assignedAt DESC). */
    List<UserRoleResponse> getUserRolesByCenterId(Integer centerId, Integer page, Integer size);

    /** List active roles of a user (ordered by assignedAt DESC). */
    List<UserRoleResponse> getUserRolesByUserId(Integer userId);

    /** List revoked roles of a user (to check which roles cannot be re-assigned). */
    List<UserRoleResponse> getRevokedRolesByUserId(Integer userId);

    /** Check if user has a role at a center (centerId may be null for GLOBAL). */
    boolean hasRoleAtCenter(Integer userId, String roleCode, Integer centerId);

    /**
     * Assign role to user if not already assigned (idempotent).
     * Used for auto-assignment of default roles.
     *
     * Rules:
     * - GLOBAL: nếu đã có GLOBAL → return; centerId = null
     * - CENTER: nếu countCenterAssignments ≥3 hoặc centerId=null → return
     * - Nếu existsByUserIdAndRoleIdAndCenterId → return
     * - Save UserRole(grantedAt=now)
     */
    void assignIfNotExists(Long userId, Integer roleId, com.example.sis.enums.RoleScope scope, Integer centerId);
}
