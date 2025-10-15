package com.example.sis.configs;

import com.example.sis.services.DefaultRoleSyncService;
import com.example.sis.services.UserLookupService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to automatically assign default roles to users after successful authentication.
 * This filter runs after BearerTokenAuthenticationFilter and ensures users have their default roles assigned.
 */
@Component
public class DefaultRoleAutoAssignFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRoleAutoAssignFilter.class);

    // Paths to exclude from auto-assignment (public endpoints, actuator, etc.)
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/public/",
        "/actuator/",
        "/swagger-ui",
        "/v3/api-docs",
        "/swagger-resources",
        "/webjars/"
    );

    private final DefaultRoleSyncService defaultRoleSyncService;
    private final UserLookupService userLookupService;

    public DefaultRoleAutoAssignFilter(DefaultRoleSyncService defaultRoleSyncService,
                                     UserLookupService userLookupService) {
        this.defaultRoleSyncService = defaultRoleSyncService;
        this.userLookupService = userLookupService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip filter for excluded paths
        for (String excludedPath : EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }

        // Skip filter for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    // Resolve user ID from token/authentication
                    Long userId = userLookupService.resolveUserIdFromToken(authentication);

                    if (userId != null) {
                        // Ensure default role is assigned
                        defaultRoleSyncService.ensureDefaultRoleAssigned(userId);
                    }
                } catch (Exception ex) {
                    // Log error but don't block the request
                    logger.error("Error during default role assignment for request {}: {}",
                               request.getRequestURI(), ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            // Log error but don't block the request
            logger.error("Unexpected error in DefaultRoleAutoAssignFilter: {}", ex.getMessage(), ex);
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}