package com.example.sis.securities;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authorization service dùng trong @PreAuthorize:
 * Ví dụ: @PreAuthorize("@authz.hasAcademicAccessForClass(authentication, #classId)")
 */
@Component("authz")
public class AuthzService {

    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private final ClassRepository classRepo;
    private final RolePermissionRepository rolePermissionRepo;

    public AuthzService(UserRoleRepository userRoleRepo,
                        RoleRepository roleRepo,
                        ClassRepository classRepo,
                        RolePermissionRepository rolePermissionRepo) {
        this.userRoleRepo = userRoleRepo;
        this.roleRepo = roleRepo;
        this.classRepo = classRepo;
        this.rolePermissionRepo = rolePermissionRepo;
    }

    // ===================== JWT Helper =====================
    public String getCurrentUserId(Authentication authentication) {
        return getSub(authentication);
    }

    private String getSub(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) return null;
        String sub = jwt.getClaimAsString("sub");
        return (sub == null || sub.isBlank()) ? null : sub;
    }

    // ===================== ROLE-LEVEL CHECKS =====================

    /** Kiểm tra có role code cụ thể không */
    public boolean hasRole(Authentication authentication, String roleCode) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        return userRoleRepo.userHasActiveRoleByKeycloakIdAndRoleCode(sub, roleCode);
    }

    /** Kiểm tra có bất kỳ role nào trong danh sách không */
    public boolean hasAnyRole(Authentication authentication, String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) return false;
        for (String roleCode : roleCodes) {
            if (hasRole(authentication, roleCode)) {
                return true;
            }
        }
        return false;
    }

    // ===================== PERMISSION-LEVEL CHECKS =====================

    /**
     * Kiểm tra user có permission cụ thể không (qua các roles của user).
     * Super Admin luôn có tất cả permissions.
     * 
     * Ví dụ sử dụng: @PreAuthorize("@authz.hasPermission(authentication, 'USER_CREATE')")
     */
    public boolean hasPermission(Authentication authentication, String permissionCode) {
        if (authentication == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        
        // Super Admin luôn có tất cả permissions
        if (isSuperAdmin(authentication)) {
            return true;
        }
        
        String sub = getSub(authentication);
        if (sub == null) return false;
        
        return rolePermissionRepo.userHasPermissionByKeycloakId(sub, permissionCode);
    }

    /** Là Super Admin? */
    public boolean isSuperAdmin(Authentication authentication) {
        return hasRole(authentication, "SUPER_ADMIN");
    }

    /** Có bất kỳ role academic nào (ACADEMIC_STAFF hoặc CENTER_MANAGER) không? */
    public boolean hasAnyAcademicRole(Authentication authentication) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        return hasRole(authentication, "ACADEMIC_STAFF") || hasRole(authentication, "CENTER_MANAGER");
    }

    /** Kiểm tra xem user hiện tại có phải là userId được truyền vào không */
    public boolean isOwnProfile(Authentication authentication, Integer userId) {
        if (userId == null) return false;
        String keycloakUserId = getSub(authentication);
        if (keycloakUserId == null) return false;
        
        // Lấy userId từ keycloakUserId
        Integer currentUserId = userRoleRepo.findUserIdByKeycloakUserId(keycloakUserId);
        return currentUserId != null && currentUserId.equals(userId);
    }

    // ===================== CENTER-LEVEL ACCESS =====================

    /**
     * Center access:
     * - SA luôn pass
     * - Ngược lại: cần CENTER_MANAGER tại center đó
     */
    public boolean hasCenterAccess(Authentication authentication, Integer centerId) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        if (isSuperAdmin(authentication)) return true;
        if (centerId == null) return false;

        List<String> allowed = List.of("CENTER_MANAGER");
        return userRoleRepo.userHasAnyActiveRoleAtCenter(sub, allowed, centerId);
    }

    /**
     * Academic access:
     * - SA luôn pass
     * - Ngược lại: cần ACADEMIC_STAFF hoặc CENTER_MANAGER tại center đó
     */
    public boolean hasAcademicAccess(Authentication authentication, Integer centerId) {
        String sub = getSub(authentication);
        if (sub == null) return false;
        if (isSuperAdmin(authentication)) return true;
        if (centerId == null) return false;

        List<String> allowed = List.of("ACADEMIC_STAFF", "CENTER_MANAGER");
        return userRoleRepo.userHasAnyActiveRoleAtCenter(sub, allowed, centerId);
    }

    /**
     * Dùng trực tiếp trong @PreAuthorize:
     *   @authz.hasAcademicAccessForClass(authentication, #classId)
     * 
     * Cho phép:
     * - Super Admin
     * - Academic Staff/Center Manager của center chứa lớp đó
     * - Lecturer được phân công vào lớp đó
     */
    public boolean hasAcademicAccessForClass(Authentication authentication, Integer classId) {
        // Super Admin luôn pass
        if (isSuperAdmin(authentication)) return true;

        // Kiểm tra Academic Staff/Center Manager
        Integer centerId = classRepo.findCenterIdByClassId(classId);
        if (hasAcademicAccess(authentication, centerId)) return true;

        // Kiểm tra Lecturer được phân công
        String sub = getSub(authentication);
        if (sub == null) return false;
        
        Integer userId = userRoleRepo.findUserIdByKeycloakUserId(sub);
        if (userId == null) return false;

        return classRepo.isLecturerAssignedToClass(userId, classId);
    }

    // ===================== USER-ROLE MANAGEMENT =====================

    /** Có thể assign role cho user ở (optional) center không? */
    public boolean canAssignUserRole(Authentication authentication, Integer roleId, Integer centerId) {
        Role role = roleRepo.findById(roleId).orElse(null);
        if (role == null) return false;

        String code = role.getCode();
        if (RoleScopeUtil.isExclusiveGlobal(code)) {
            // GLOBAL role: chỉ SA, centerId phải null
            return centerId == null && isSuperAdmin(authentication);
        }
        // CENTER role: SA hoặc CM của center đó
        return centerId != null && hasCenterAccess(authentication, centerId);
    }

    /** Bulk guard cho assign nhiều roles */
    public boolean canAssignUserRoles(Authentication authentication, List<UserRoleRequest> requests) {
        if (requests == null || requests.isEmpty()) return false;
        for (UserRoleRequest r : requests) {
            if (r == null || r.getRoleId() == null) return false;
            Integer centerId = r.getCenterId();
            if (!canAssignUserRole(authentication, r.getRoleId(), centerId))
                return false;
        }
        return true;
    }

    /** Có thể revoke userRole theo id? */
    public boolean canModifyUserRole(Authentication authentication, Integer userRoleId) {
        Integer centerId = userRoleRepo.findCenterIdByUserRoleId(userRoleId);
        if (centerId == null)
            return isSuperAdmin(authentication);
        return hasCenterAccess(authentication, centerId);
    }

    // ===================== LEGACY SUPPORT =====================
    public boolean canListUsers(Authentication authentication, Integer centerId) {
        return hasCenterAccess(authentication, centerId);
    }

    /** Quản lý lớp học (SA hoặc Academic/CENTER_MANAGER tại center đó) */
    public boolean canManageClasses(Authentication authentication, Integer centerId) {
        return hasAcademicAccess(authentication, centerId);
    }
}
