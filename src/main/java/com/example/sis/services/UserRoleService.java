package com.example.sis.services;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;

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

    /** Soft revoke ONE user-role by id (no-op if already revoked). */
    void revokeRoleFromUser(Integer userRoleId, String revokedBy);

    /** Soft revoke MANY user-roles by ids (bulk & efficient). */
    void revokeRolesFromUsers(List<Integer> userRoleIds, String revokedBy);

    /** List active user-roles in a center (paged, ordered by assignedAt DESC). */
    List<UserRoleResponse> getUserRolesByCenterId(Integer centerId, Integer page, Integer size);

    /** List active roles of a user (ordered by assignedAt DESC). */
    List<UserRoleResponse> getUserRolesByUserId(Integer userId);

    /** Check if user has a role at a center (centerId may be null for GLOBAL). */
    boolean hasRoleAtCenter(Integer userId, String roleCode, Integer centerId);
}
