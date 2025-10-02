package com.example.sis.securities;

import com.example.sis.repositories.UserRoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("authz")
public class AuthzService {

    private final UserRoleRepository userRoleRepo;

    public AuthzService(UserRoleRepository userRoleRepo) {
        this.userRoleRepo = userRoleRepo;
    }

    private String getSub(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) return null;
        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) return null;
        return sub;
    }

    /** Check người dùng có 1 role code (role code là dữ liệu trong DB: roles.code) */
    public boolean hasRole(Authentication authentication, String roleCode) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        return userRoleRepo.userHasActiveRoleByKeycloakIdAndRoleCode(sub, roleCode);
    }

    /** Super Admin? */
    public boolean isSuperAdmin(Authentication authentication) {
        return hasRole(authentication, "SUPER_ADMIN");
    }

    /** Có quyền theo center? (SA luôn pass; nếu có centerId thì cho phép CENTER_MANAGER/ACADEMIC_STAFF của center đó) */
    public boolean hasCenterAccess(Authentication authentication, Integer centerId) {
        String sub = getSub(authentication);
        if (sub == null) return false;

        if (hasRole(authentication, "SUPER_ADMIN")) return true;
        if (centerId == null) return false;

        List<String> allowed = List.of("CENTER_MANAGER", "ACADEMIC_STAFF");
        return userRoleRepo.userHasAnyActiveRoleAtCenter(sub, allowed, centerId);
    }

    /** Rule dùng riêng cho API list users (giữ lại vì bạn đã gọi ở controller) */
    public boolean canListUsers(Authentication authentication, Integer centerId) {
        return hasCenterAccess(authentication, centerId);
    }
}
