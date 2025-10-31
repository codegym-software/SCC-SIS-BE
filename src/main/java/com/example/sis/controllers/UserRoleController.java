package com.example.sis.controllers;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.dtos.userrole.UserRoleAssignmentSummaryResponse;
import com.example.sis.services.UserRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;   // <-- IMPORTANT
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User ↔ Role assignments (GLOBAL or per-center).
 * Base path: /api/user-roles
 */
@RestController
@RequestMapping("/api/user-roles")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    /** Assign ONE role. */
    @PostMapping
    @PreAuthorize("@authz.canAssignUserRole(authentication, #req.roleId, #req.centerId)")
    public ResponseEntity<UserRoleResponse> assignRoleToUser(
            @P("req") @Valid @RequestBody UserRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String assignedBy = jwt.getSubject();
        UserRoleResponse res = userRoleService.assignRoleToUser(request, assignedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * Assign MANY roles for a single user with summary response.
     * Path userId is applied to all items.
     * Returns summary with createdCount, skippedCount, errors.
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("@authz.canAssignUserRoles(authentication, #items)")
    public ResponseEntity<UserRoleAssignmentSummaryResponse> assignRolesToUserWithSummary(
            @PathVariable Integer userId,
            @P("items") @RequestBody List<UserRoleRequest> requests,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (requests != null) {
            for (UserRoleRequest r : requests) {
                if (r != null) r.setUserId(userId);
            }
        }
        UserRoleAssignmentSummaryResponse summary = userRoleService.assignRolesToUserWithSummary(userId, requests, jwt.getSubject());
        HttpStatus status = (summary.getCreatedCount() > 0) ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(summary);
    }

    /** Revoke ONE (soft). */
    @DeleteMapping("/{userRoleId}")
    @PreAuthorize("@authz.canModifyUserRole(authentication, #userRoleId)")
    public ResponseEntity<Void> revokeRoleFromUser(
            @PathVariable Integer userRoleId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userRoleService.revokeRoleFromUser(userRoleId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /** Revoke MANY (bulk, SA only). */
    @DeleteMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<Void> revokeRolesFromUsers(
            @RequestBody List<Integer> userRoleIds,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userRoleService.revokeRolesFromUsers(userRoleIds, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /** List active assignments in a center (paged). */
    @GetMapping("/center/{centerId}")
    @PreAuthorize("@authz.hasCenterAccess(authentication, #centerId)")
    public ResponseEntity<List<UserRoleResponse>> getUserRolesByCenterId(
            @PathVariable Integer centerId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size
    ) {
        return ResponseEntity.ok(userRoleService.getUserRolesByCenterId(centerId, page, size));
    }

    /** List active roles of a user (all, SA only). */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<List<UserRoleResponse>> getUserRolesByUserId(
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(userRoleService.getUserRolesByUserId(userId));
    }

    /** List revoked roles of a user (to check which roles cannot be re-assigned). */
    @GetMapping("/user/{userId}/revoked")
    @PreAuthorize("@authz.canListUsers(authentication, null)")
    public ResponseEntity<List<UserRoleResponse>> getRevokedRolesByUserId(
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(userRoleService.getRevokedRolesByUserId(userId));
    }
}
