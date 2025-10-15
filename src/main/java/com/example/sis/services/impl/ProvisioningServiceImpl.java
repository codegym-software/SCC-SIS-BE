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

            // Check if user already exists
            return userRepository.findIdByKeycloakUserId(sub).orElseGet(() -> {
                logger.info("Creating new user from JWT token with sub: {}", sub);

                // Extract claims from JWT token
                String email = jwt.getClaimAsString("email");
                String firstName = jwt.getClaimAsString("given_name");
                String lastName = jwt.getClaimAsString("family_name");
                String username = jwt.getClaimAsString("preferred_username");

                // Build full name from available claims
                String fullName = buildFullName(lastName, firstName, username);

                // Create new user
                User user = new User();
                user.setKeycloakUserId(sub);
                user.setEmail(email);
                user.setFullName(fullName);
                user.setPhone(""); // Default empty phone
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
     * Build full name from JWT claims with fallback logic
     */
    private String buildFullName(String lastName, String firstName, String username) {
        StringBuilder fullNameBuilder = new StringBuilder();

        // Add last name first if available
        if (lastName != null && !lastName.trim().isEmpty()) {
            fullNameBuilder.append(lastName.trim());
        }

        // Add first name with space if last name exists
        if (firstName != null && !firstName.trim().isEmpty()) {
            if (fullNameBuilder.length() > 0) {
                fullNameBuilder.append(" ");
            }
            fullNameBuilder.append(firstName.trim());
        }

        // Fallback to username if no first/last name available
        if (fullNameBuilder.length() == 0 && username != null && !username.trim().isEmpty()) {
            fullNameBuilder.append(username.trim());
        }

        String fullName = fullNameBuilder.toString().trim();

        // Final fallback if still empty
        if (fullName.isEmpty()) {
            fullName = "Unknown User";
        }

        return fullName;
    }
}