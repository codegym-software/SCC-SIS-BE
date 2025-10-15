package com.example.sis.services.impl;

import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.ProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of ProvisioningService for auto-creating users from JWT tokens
 */
@Service
public class ProvisioningServiceImpl implements ProvisioningService {

    private static final Logger logger = LoggerFactory.getLogger(ProvisioningServiceImpl.class);

    private final UserRepository userRepository;

    public ProvisioningServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Integer ensureUserExists(Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String sub = jwt.getClaimAsString("sub");

            if (sub == null) {
                logger.warn("JWT token missing 'sub' claim for user provisioning");
                return null;
            }

            // Check if user already exists by keycloak user ID
            return userRepository.findIdByKeycloakUserId(sub).orElseGet(() -> {
                logger.info("User not found by keycloak ID, checking for email conflicts for sub: {}", sub);

                // Extract claims from JWT token
                String email = jwt.getClaimAsString("email");
                String firstName = jwt.getClaimAsString("given_name");  // tên
                String lastName = jwt.getClaimAsString("family_name");  // họ
                String username = jwt.getClaimAsString("preferred_username");
                String nameClaim = jwt.getClaimAsString("name");        // fallback name claim

                // Build full name in Vietnamese style: family_name + given_name
                String fullName = buildFullName(lastName, firstName, nameClaim, username, email);

                // Check if there's an existing user with same email but no keycloak_user_id
                if (email != null && !email.trim().isEmpty()) {
                    User existingUser = userRepository.findByEmail(email).orElse(null);
                    if (existingUser != null) {
                        // Check if existing user already has a different keycloak_user_id
                        if (existingUser.getKeycloakUserId() != null &&
                            !existingUser.getKeycloakUserId().equals(sub)) {
                            logger.warn("Email {} already associated with different Keycloak user ID: {} (current: {})",
                                email, existingUser.getKeycloakUserId(), sub);
                            throw new RuntimeException("Email already associated with different Keycloak user");
                        }

                        // Update existing user with keycloak_user_id
                        logger.info("Updating existing user {} with Keycloak ID: {}", existingUser.getUserId(), sub);
                        existingUser.setKeycloakUserId(sub);
                        existingUser.setFullName(fullName);
                        existingUser.setPhone(""); // Default empty phone for auto-provisioned users
                        existingUser.setUpdatedAt(LocalDateTime.now());

                        User savedUser = userRepository.save(existingUser);
                        logger.info("Successfully updated existing user with ID: {} for Keycloak user: {}", savedUser.getUserId(), sub);
                        return savedUser.getUserId();
                    }
                }

                // Create new user
                logger.info("Creating new user from JWT token with sub: {}", sub);
                User user = new User();
                user.setKeycloakUserId(sub);
                user.setEmail(email);
                user.setFullName(fullName);
                user.setPhone(""); // Default empty phone for auto-provisioned users
                user.setActive(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                User savedUser = userRepository.save(user);
                logger.info("Successfully created new user with ID: {} for Keycloak user: {}", savedUser.getUserId(), sub);

                return savedUser.getUserId();
            });

        } catch (Exception e) {
            logger.error("Error provisioning user from JWT token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build full name from JWT claims with Vietnamese name order: family_name + given_name
     * Fallback order: family_name + given_name → name → preferred_username → email → "Unknown User"
     *
     * Ví dụ:
     * - family_name="Nguyễn", given_name="Quỳnh Như" → "Nguyễn Quỳnh Như"
     * - name="Nguyễn Văn An" → "Nguyễn Văn An"
     * - preferred_username="nguyenvana" → "nguyenvana"
     * - email="a@edu.vn" → "a"
     *
     * @param lastName family_name claim (họ)
     * @param firstName given_name claim (tên)
     * @param nameClaim name claim (tên đầy đủ)
     * @param username preferred_username claim
     * @param email email claim
     * @return formatted full name in Vietnamese style (họ trước tên)
     */
    private String buildFullName(String lastName, String firstName, String nameClaim, String username, String email) {
        StringBuilder fullNameBuilder = new StringBuilder();

        // Primary: Build Vietnamese style name - family_name (họ) + given_name (tên)
        // Ví dụ: "Nguyễn Quỳnh Như"
        if (lastName != null && !lastName.trim().isEmpty()) {
            fullNameBuilder.append(lastName.trim());
        }

        if (firstName != null && !firstName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) {
                fullNameBuilder.append(" ");
            }
            fullNameBuilder.append(firstName.trim());
        }

        // Fallback 1: Use 'name' claim if available (some providers use this for full name)
        if (fullNameBuilder.length() == 0 && nameClaim != null && !nameClaim.trim().isEmpty()) {
            fullNameBuilder.append(nameClaim.trim());
        }

        // Fallback 2: Use preferred_username
        if (fullNameBuilder.length() == 0 && username != null && !username.trim().isEmpty()) {
            fullNameBuilder.append(username.trim());
        }

        // Fallback 3: Use email username part (before @)
        if (fullNameBuilder.length() == 0 && email != null && !email.trim().isEmpty()) {
            String emailUsername = email.substring(0, email.indexOf('@'));
            if (!emailUsername.trim().isEmpty()) {
                fullNameBuilder.append(emailUsername.trim());
            }
        }

        String fullName = fullNameBuilder.toString().trim();

        // Final fallback if still empty
        if (fullName.isEmpty()) {
            fullName = "Unknown User";
        }

        return fullName;
    }
}