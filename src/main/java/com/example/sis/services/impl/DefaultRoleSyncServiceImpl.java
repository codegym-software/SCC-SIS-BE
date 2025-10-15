package com.example.sis.services.impl;

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

    public DefaultRoleSyncServiceImpl(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   UserRoleRepository userRoleRepository,
                                   UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRoleService = userRoleService;
    }

    /**
     * Ensures that a user has their default role assigned if they don't already have role assignments.
     * This method is idempotent and follows the business rules:
     * - If user already has role assignments → return
     * - Get defaultRoleId (+ defaultCenterId) from users table
     * - role = findActiveById(defaultRoleId).orElse(null) → null thì return
     * - scope=GLOBAL → assignIfNotExists(userId, roleId, GLOBAL, null)
     * - scope=CENTER → nếu defaultCenterId!=null → assignIfNotExists(userId, roleId, CENTER, defaultCenterId)
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

            // Check if user has default role configured
            Integer defaultRoleId = user.getDefaultRoleId();
            if (defaultRoleId == null) {
                logger.debug("User {} has no default role configured, skipping assignment", userId);
                return;
            }

            // Get the role and validate it's active
            Optional<Role> roleOpt = roleRepository.findActiveById(defaultRoleId);
            if (roleOpt.isEmpty()) {
                logger.warn("Default role {} for user {} is not found or inactive, skipping assignment",
                           defaultRoleId, userId);
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
                userRoleService.assignIfNotExists(userId, defaultRoleId, RoleScope.GLOBAL, null);
            } else if (RoleScope.CENTER.equals(scope)) {
                Integer defaultCenterId = user.getDefaultCenterId();
                if (defaultCenterId != null) {
                    logger.info("Assigning CENTER role {} with center {} to user {}",
                               role.getName(), defaultCenterId, userId);
                    userRoleService.assignIfNotExists(userId, defaultRoleId, RoleScope.CENTER, defaultCenterId);
                } else {
                    logger.warn("User {} has CENTER role {} but no default center configured, skipping assignment",
                               userId, role.getName());
                }
            }

        } catch (Exception ex) {
            // Log error but don't throw exception to avoid blocking requests
            logger.error("Error while assigning default role to user {}: {}", userId, ex.getMessage(), ex);
        }
    }
}