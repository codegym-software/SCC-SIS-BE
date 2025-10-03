package com.example.sis.services;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;

import java.util.List;

/**
 * Manage Role ↔ Permission assignments (Super Admin only).
 * Filtered & paged listing, idempotent assign, and revoke.
 */
public interface RolePermissionService {

    /** List assigned permissions of a role (filtered & paged; active only). */
    List<PermissionResponse> listAssigned(Integer roleId, String q, String category,
                                          Integer page, Integer size, String sort);

    /** List unassigned permissions (filtered & paged; active only). */
    List<PermissionResponse> listUnassigned(Integer roleId, String q, String category,
                                            Integer page, Integer size, String sort);

    /** Assign ONE permission to role (idempotent). */
    RolePermissionResponse assignPermissionToRole(RolePermissionRequest request, String grantedBy);

    /** Assign MANY permissions to role (idempotent per item). */
    List<RolePermissionResponse> assignMultiplePermissionsToRole(Integer roleId, List<Integer> permissionIds,
                                                                 String grantedBy);

    /** Revoke ONE permission from role. */
    void revokePermissionFromRole(Integer roleId, Integer permissionId);

    /** Revoke MANY permissions from role (bulk, efficient). */
    void revokeMultiplePermissionsFromRole(Integer roleId, List<Integer> permissionIds);
}
