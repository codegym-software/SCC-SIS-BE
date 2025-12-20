package com.example.sis.security;

import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.StudentClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolve user context from JWT token
 * Extract userId, roles, class info without exposing sensitive data
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityContextResolver {
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentClassService studentClassService;
    
    /**
     * Get current authenticated user ID from JWT
     * Maps Keycloak UUID (sub) to database userId by querying users table
     */
    public Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        if (auth.getPrincipal() instanceof Jwt jwt) {
            // Get Keycloak user ID from JWT sub claim
            String keycloakUserId = jwt.getClaimAsString("sub");
            
            // Try custom claim first (if your Keycloak adds user_id)
            Integer customUserId = jwt.getClaim("user_id");
            if (customUserId != null) {
                return customUserId;
            }
            
            // Query database to get userId from keycloak_user_id
            User user = userRepository.findByKeycloakUserId(keycloakUserId)
                    .orElseThrow(() -> new SecurityException("User not found in database: " + keycloakUserId));
            
            return user.getUserId();
        }
        
        throw new SecurityException("Invalid authentication principal");
    }
    
    /**
     * Get current user email
     */
    public String getCurrentUserEmail() {
        Jwt jwt = getCurrentJwt();
        return jwt.getClaimAsString("email");
    }
    
    /**
     * Get current user preferred username
     */
    public String getCurrentUsername() {
        Jwt jwt = getCurrentJwt();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) {
            username = jwt.getClaimAsString("name");
        }
        return username != null ? username : "Unknown";
    }
    
    /**
     * Get current user roles
     */
    public Set<String> getCurrentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replace("ROLE_", ""))
            .collect(Collectors.toSet());
    }
    
    /**
     * Check if current user has role
     */
    public boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role.toUpperCase());
    }
    
    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * Check if current user is teacher
     */
    public boolean isTeacher() {
        return hasRole("TEACHER");
    }
    
    /**
     * Check if current user is student
     */
    public boolean isStudent() {
        return hasRole("STUDENT");
    }
    
    /**
     * Get SAFE user context for chat (NO TOKENS, NO PASSWORDS)
     * Only public info: userId, username, roles
     * Fetches student/enrollment info from database if user is a student
     */
    public Map<String, Object> getSafeChatContext() {
        Jwt jwt = getCurrentJwt();
        Integer userId = getCurrentUserId();
        
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("username", getCurrentUsername());
        context.put("email", getCurrentUserEmail());
        context.put("roles", getCurrentUserRoles());
        
        // Add custom claims if exist (classId, studentId from JWT)
        if (jwt.hasClaim("class_id")) {
            context.put("classId", jwt.getClaim("class_id"));
        }
        if (jwt.hasClaim("student_id")) {
            context.put("studentId", jwt.getClaim("student_id"));
        }
        if (jwt.hasClaim("center_id")) {
            context.put("centerId", jwt.getClaim("center_id"));
        }
        
        // Always try to fetch student info from database (regardless of role)
        // This handles cases where Keycloak roles might not be synced
        try {
            studentRepository.findByUserIdAndDeletedAtIsNull(userId).ifPresent(student -> {
                context.put("scope", "PUBLIC");
                    context.put("studentId", student.getStudentId());
                    context.put("fullName", student.getFullName());
                    context.put("email", student.getEmail());
                    context.put("phone", student.getPhone());
                    context.put("status", student.getOverallStatus().name());
                    
                    // Get classes using StudentClassService
                    try {
                        List<ClassResponse> classes = studentClassService.getClassesByStudentId(student.getStudentId());
                        
                        if (!classes.isEmpty()) {
                            var classesInfo = classes.stream()
                                .map(c -> {
                                    Map<String, Object> classMap = new HashMap<>();
                                    classMap.put("classId", c.getClassId());
                                    classMap.put("className", c.getName());
                                    classMap.put("programName", c.getProgramName() != null ? c.getProgramName() : "N/A");
                                    classMap.put("centerName", c.getCenterName() != null ? c.getCenterName() : "N/A");
                                    classMap.put("status", c.getStatus().name());
                                    classMap.put("startDate", c.getStartDate() != null ? c.getStartDate().toString() : "N/A");
                                    classMap.put("endDate", c.getEndDate() != null ? c.getEndDate().toString() : "N/A");
                                    classMap.put("studyDays", c.getStudyDays() != null ? c.getStudyDays() : "N/A");
                                    classMap.put("studyTime", c.getStudyTime() != null ? c.getStudyTime() : "N/A");
                                    classMap.put("room", c.getRoom() != null ? c.getRoom() : "N/A");
                                    return classMap;
                                })
                                .toList();
                            
                            context.put("classes", classesInfo);
                            log.info("📚 Fetched {} classes for studentId={}", classesInfo.size(), student.getStudentId());
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to fetch classes for studentId={}: {}", student.getStudentId(), e.getMessage());
                    }
                    
                    log.info("📚 Fetched student info: studentId={}, name={}, status={}", 
                        student.getStudentId(), student.getFullName(), student.getOverallStatus());
                });
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch student info for userId={}: {}", userId, e.getMessage());
        }
        
        // Set scope based on roles (fallback if not already set)
        if (context.get("scope") == null) {
            if (isTeacher()) {
                context.put("scope", "TEACHER");
            } else if (isAdmin()) {
                context.put("scope", "ALL");
            }
        }
        
        log.info("🔐 User context: userId={}, username={}, roles={}, scope={}", 
            userId, getCurrentUsername(), getCurrentUserRoles(), context.get("scope"));
        
        return context;
    }
    
    /**
     * Verify user owns resource
     */
    public void verifyOwnership(Integer resourceUserId) {
        Integer currentUserId = getCurrentUserId();
        
        if (!currentUserId.equals(resourceUserId) && !isAdmin()) {
            throw new SecurityException(
                String.format("User %d does not have permission to access resource owned by user %d", 
                    currentUserId, resourceUserId)
            );
        }
    }
    
    // Helper
    private Jwt getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        
        throw new SecurityException("JWT token not found in security context");
    }
}
