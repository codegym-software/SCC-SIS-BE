package com.example.sis.services.impl;

import com.example.sis.configs.AuthProps;
import com.example.sis.enums.RoleScope;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Role;
import com.example.sis.models.User;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.DefaultRoleSyncService;
import com.example.sis.services.UserRoleService;
import com.example.sis.utils.RoleScopeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of DefaultRoleSyncService for auto-assigning default roles to users
 */
@Service
@Transactional
public class DefaultRoleSyncServiceImpl implements DefaultRoleSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRoleSyncServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleService userRoleService;
    private final AuthProps authProps;

    public DefaultRoleSyncServiceImpl(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    UserRoleRepository userRoleRepository,
                                    UserRoleService userRoleService,
                                    AuthProps authProps) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRoleService = userRoleService;
        this.authProps = authProps;
    }

    /**
     * Ensures that a user has their default role assigned if they don't already have role assignments.
     * Policy: user preference > config default
     * - If !autoAssignEnabled → return early
     * - If user already has role assignments → return
     * - Get user.defaultRoleId if exists; otherwise use config defaultRoleCode
     * - If scope=CENTER and user.defaultCenterId==null → use config defaultCenterId if available
     * - Respect rules & idempotent: GLOBAL ≤1, CENTER ≤3, no duplicates
     * - Don't throw; only log.warn when missing config/role inactive
     *
     * @param userId The user ID to check and assign default role for
     */
    @Override
    @Transactional
    public void ensureDefaultRoleAssigned(Long userId) {
        if (userId == null) {
            logger.debug("User ID is null, skipping default role assignment");
            return;
        }

        // Điều kiện kích hoạt: nếu không bật auto-assign thì return sớm
        if (!authProps.isAutoAssignEnabled()) {
            logger.debug("Auto-assign is disabled, skipping default role assignment for user {}", userId);
            return;
        }

        try {
            // Check if user already has any role assignments
            if (userRoleRepository.existsByUserId(userId)) {
                logger.debug("User {} already has role assignments, skipping default role assignment", userId);
                return;
            }

            // Get user with default role information
            Integer userIdInt;
            try {
                userIdInt = Math.toIntExact(userId);
            } catch (ArithmeticException ex) {
                logger.warn("Cannot convert userId {} to Integer, skipping default role assignment", userId);
                return;
            }

            Optional<User> userOpt = userRepository.findById(userIdInt);
            if (userOpt.isEmpty()) {
                logger.warn("User not found with ID: {}, skipping default role assignment", userId);
                return;
            }

            User user = userOpt.get();

            // Policy: lấy default role (ưu tiên user > config)
            Integer roleId = null;
            if (user.getDefaultRoleId() != null) {
                // User có cấu hình default role
                roleId = user.getDefaultRoleId();
                logger.debug("Using user's default role ID: {} for user {}", roleId, userId);
            } else {
                // User không có default role → dùng config defaultRoleCode
                String defaultRoleCode = authProps.getDefaultRoleCode();
                if (defaultRoleCode == null || defaultRoleCode.trim().isEmpty()) {
                    logger.warn("No default role code configured in auth props, skipping assignment for user {}", userId);
                    return;
                }

                Optional<Integer> roleIdOpt = roleRepository.findIdByCode(defaultRoleCode);
                if (roleIdOpt.isEmpty()) {
                    logger.warn("Default role with code '{}' not found or inactive, skipping assignment for user {}",
                               defaultRoleCode, userId);
                    return;
                }

                roleId = roleIdOpt.get();
                logger.debug("Using config default role code '{}' (ID: {}) for user {}", defaultRoleCode, roleId, userId);
            }

            // Validate role exists and is active
            Optional<Role> roleOpt = roleRepository.findActiveById(roleId);
            if (roleOpt.isEmpty()) {
                logger.warn("Role with ID {} is not found or inactive, skipping assignment for user {}", roleId, userId);
                return;
            }

            Role role = roleOpt.get();

            // Determine role scope
            String roleCode = role.getCode();
            RoleScope scope;
            try {
                scope = RoleScopeUtil.resolveScope(roleCode);
            } catch (IllegalArgumentException ex) {
                logger.warn("Unknown role code {} for user {}, skipping assignment", roleCode, userId);
                return;
            }

            // Assign role based on scope
            if (RoleScope.GLOBAL.equals(scope)) {
                logger.info("Assigning GLOBAL role {} to user {}", role.getName(), userId);
                userRoleService.assignIfNotExists(userId, roleId, RoleScope.GLOBAL, null);
            } else if (RoleScope.CENTER.equals(scope)) {
                // Nếu user.defaultCenterId==null → dùng config defaultCenterId nếu có
                Integer centerId = user.getDefaultCenterId();
                if (centerId == null) {
                    centerId = authProps.getDefaultCenterId();
                    if (centerId == null) {
                        logger.warn("User {} has CENTER role {} but no default center configured (neither user nor config), skipping assignment",
                                   userId, role.getName());
                        return;
                    }
                    logger.debug("Using config default center ID: {} for user {}", centerId, userId);
                } else {
                    logger.debug("Using user's default center ID: {} for user {}", centerId, userId);
                }

                logger.info("Assigning CENTER role {} with center {} to user {}", role.getName(), centerId, userId);
                userRoleService.assignIfNotExists(userId, roleId, RoleScope.CENTER, centerId);
            }

        } catch (Exception ex) {
            // Log error but don't throw exception to avoid blocking requests
            logger.error("Error while assigning default role to user {}: {}", userId, ex.getMessage(), ex);
        }
    }
}