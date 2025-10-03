package com.example.sis.securities;

import com.example.sis.dtos.userrole.UserRoleRequest; // NEW
import com.example.sis.models.Role;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("authz")
public class AuthzService {

    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;

    public AuthzService(UserRoleRepository userRoleRepo, RoleRepository roleRepo) {
        this.userRoleRepo = userRoleRepo;
        this.roleRepo = roleRepo;
    }

    private String getSub(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) return null;
        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) return null;
        return sub;
    }

    /** Has a specific role code? (roles.code in DB) */
    public boolean hasRole(Authentication authentication, String roleCode) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        return userRoleRepo.userHasActiveRoleByKeycloakIdAndRoleCode(sub, roleCode);
    }

    /** Super Admin? */
    public boolean isSuperAdmin(Authentication authentication) {
        return hasRole(authentication, "SUPER_ADMIN");
    }

    /**
     * Center access:
     * - SA always pass
     * - else: must be CENTER_MANAGER at that center
     */
    public boolean hasCenterAccess(Authentication authentication, Integer centerId) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        if (isSuperAdmin(authentication)) return true;
        if (centerId == null) return false;

        List<String> allowed = List.of("CENTER_MANAGER");
        return userRoleRepo.userHasAnyActiveRoleAtCenter(sub, allowed, centerId);
    }

    /** Legacy rule (kept) */
    public boolean canListUsers(Authentication authentication, Integer centerId) {
        return hasCenterAccess(authentication, centerId);
    }

    /**
     * Can assign a role to user at (optional) center?
     * - GLOBAL role: centerId must be null, and only SA can assign
     * - CENTER role: centerId required; SA or CM(centerId)
     */
    public boolean canAssignUserRole(Authentication authentication, Integer roleId, Integer centerId) {
        Role role = roleRepo.findById(roleId).orElse(null);
        if (role == null) return false;

        String code = role.getCode();
        if (RoleScopeUtil.isExclusiveGlobal(code)) {
            return centerId == null && isSuperAdmin(authentication);
        }
        // default center-scoped
        return centerId != null && hasCenterAccess(authentication, centerId);
    }

    /**
     * Bulk guard: every item must satisfy canAssignUserRole(...).
     */
    public boolean canAssignUserRoles(Authentication authentication, List<UserRoleRequest> requests) {
        if (requests == null || requests.isEmpty()) return false;
        for (UserRoleRequest r : requests) {
            if (r == null || r.getRoleId() == null) return false;
            Integer centerId = r.getCenterId();
            if (!canAssignUserRole(authentication, r.getRoleId(), centerId)) return false;
        }
        return true;
    }

    /**
     * Can modify (revoke) a userRole by id?
     * - GLOBAL assignment (centerId=null) → SA only
     * - CENTER assignment → SA or CM(centerId)
     */
    public boolean canModifyUserRole(Authentication authentication, Integer userRoleId) {
        Integer centerId = userRoleRepo.findCenterIdByUserRoleId(userRoleId);
        if (centerId == null) return isSuperAdmin(authentication);
        return hasCenterAccess(authentication, centerId);
    }
}
