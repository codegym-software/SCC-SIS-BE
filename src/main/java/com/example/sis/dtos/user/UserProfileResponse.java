package com.example.sis.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for user profile response containing user info, Keycloak claims, and active roles
 */
public class UserProfileResponse {

    @JsonProperty("userId")
    private Integer userId;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("keycloak")
    private KeycloakInfo keycloak;

    @JsonProperty("roles")
    private List<RoleInfo> roles;

    public UserProfileResponse() {}

    public UserProfileResponse(Integer userId, String fullName, String email,
                              KeycloakInfo keycloak, List<RoleInfo> roles) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.keycloak = keycloak;
        this.roles = roles;
    }

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public KeycloakInfo getKeycloak() { return keycloak; }
    public void setKeycloak(KeycloakInfo keycloak) { this.keycloak = keycloak; }

    public List<RoleInfo> getRoles() { return roles; }
    public void setRoles(List<RoleInfo> roles) { this.roles = roles; }

    /**
     * Keycloak information from JWT token
     */
    public static class KeycloakInfo {
        @JsonProperty("username")
        private String username;

        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("lastName")
        private String lastName;

        public KeycloakInfo() {}

        public KeycloakInfo(String username, String firstName, String lastName) {
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    /**
     * Role information with code and scope
     */
    public static class RoleInfo {
        @JsonProperty("code")
        private String code;

        @JsonProperty("scope")
        private String scope;

        public RoleInfo() {}

        public RoleInfo(String code, String scope) {
            this.code = code;
            this.scope = scope;
        }

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
    }
}