package com.example.sis.controllers;

import com.example.sis.dtos.user.CreateUserRequest;
import com.example.sis.dtos.user.UserResponse;
import com.example.sis.dtos.user.UserProfileResponse;
import com.example.sis.models.User;
import com.example.sis.models.UserRole;
import com.example.sis.models.Role;
import com.example.sis.models.Center;
import com.example.sis.services.UserLookupService;
import com.example.sis.services.UserService;
import com.example.sis.services.DefaultRoleSyncService;
import com.example.sis.services.ProvisioningService;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.enums.RoleScope;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserLookupService userLookupService;
    private final DefaultRoleSyncService defaultRoleSyncService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProvisioningService provisioningService;

    public UserController(UserService userService,
            UserLookupService userLookupService,
            DefaultRoleSyncService defaultRoleSyncService,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            ProvisioningService provisioningService) {
        this.userService = userService;
        this.userLookupService = userLookupService;
        this.defaultRoleSyncService = defaultRoleSyncService;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.provisioningService = provisioningService;
    }

    // CHỈ SUPER_ADMIN (trong DB) mới được tạo user
    @PostMapping
    @PreAuthorize("@authz.hasRole(authentication, 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    // SA: xem toàn hệ thống hoặc center bất kỳ
    // CENTER_MANAGER/ACADEMIC_STAFF: chỉ xem được trong center của mình
    @GetMapping
    @PreAuthorize("@authz.canListUsers(authentication, #centerId)")
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(value = "centerId", required = false) Integer centerId) {
        return ResponseEntity.ok(userService.getUsers(centerId));
    }

    // API GET /api/profile - trả về thông tin user hiện tại và danh sách role
    // active
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        // 1. Lấy userId từ token (cố gắng tìm user hiện có)
        Long userIdLong = userLookupService.resolveUserIdFromToken(authentication);

        // 2. Nếu không tìm thấy user hiện có, thử tạo user mới từ JWT token
        Integer userId = (userIdLong != null) ? userIdLong.intValue()
                : provisioningService.ensureUserExists(authentication);

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        // 3. Đảm bảo default role được gán (idempotent)
        defaultRoleSyncService.ensureDefaultRoleAssigned(Long.valueOf(userId));

        // 4. Lấy thông tin user từ DB
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        // 5. Lấy thông tin Keycloak từ JWT token
        UserProfileResponse.KeycloakInfo keycloakInfo = extractKeycloakInfo(authentication);

        // 6. Lấy danh sách roles active của user
        List<UserRole> activeRoles = userRoleRepository.findActiveByUserId(userId);
        List<UserProfileResponse.RoleInfo> roleInfos = activeRoles.stream()
                .map(userRole -> {
                    String scope = userRole.getCenter() == null ? "GLOBAL" : "CENTER";
                    return new UserProfileResponse.RoleInfo(userRole.getRole().getCode(), scope);
                })
                .collect(Collectors.toList());

        // 7. Lấy centerId và centerName nếu user có CENTER scope
        Integer centerId = null;
        String centerName = null;

        // Tìm role đầu tiên có CENTER scope (không phải GLOBAL)
        UserRole centerRole = activeRoles.stream()
                .filter(userRole -> userRole.getCenter() != null)
                .findFirst()
                .orElse(null);

        if (centerRole != null) {
            centerId = centerRole.getCenter().getCenterId();
            centerName = centerRole.getCenter().getName();
        }

        // 8. Tạo response
        UserProfileResponse response = new UserProfileResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                keycloakInfo,
                roleInfos,
                centerId,
                centerName);

        return ResponseEntity.ok(response);
    }

    /**
     * Extract Keycloak information from JWT token
     */
    private UserProfileResponse.KeycloakInfo extractKeycloakInfo(Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String username = jwt.getClaimAsString("preferred_username");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            return new UserProfileResponse.KeycloakInfo(username, firstName, lastName);
        } catch (Exception e) {
            // Fallback nếu không thể lấy thông tin từ JWT
            return new UserProfileResponse.KeycloakInfo(null, null, null);
        }
    }
}
