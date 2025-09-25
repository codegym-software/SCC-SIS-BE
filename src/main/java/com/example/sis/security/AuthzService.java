// src/main/java/com/example/sis/security/AuthzService.java
package com.example.sis.security;

import com.example.sis.repository.UserRoleRepository;
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

    public boolean hasRole(Authentication authentication, String roleCode) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) return false;
        return userRoleRepo.userHasActiveRoleByKeycloakIdAndRoleCode(sub, roleCode);
    }

    // NEW: rule list users
    public boolean canListUsers(Authentication authentication, Integer centerId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) return false;

        // SA luôn được
        if (userRoleRepo.userHasActiveRoleByKeycloakIdAndRoleCode(sub, "SUPER_ADMIN")) return true;

        // Không centerId -> chỉ SA được xem toàn hệ thống
        if (centerId == null) return false;

        // CENTER_MANAGER hoặc ACADEMIC_STAFF của center đó thì cho phép
        List<String> allowed = List.of("CENTER_MANAGER", "ACADEMIC_STAFF");
        return userRoleRepo.userHasAnyActiveRoleAtCenter(sub, allowed, centerId);
    }
}
