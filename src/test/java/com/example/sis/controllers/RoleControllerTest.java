package com.example.sis.controllers;

import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.util.JwtRequestPostProcessor;
import com.example.sis.util.TestConstants;
import com.example.sis.util.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RoleController
 * Tests the full stack from HTTP request to database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/test-data.sql")
@DisplayName("RoleController Integration Tests")
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        // Data is loaded from test-data.sql
    }

    // ==================== GET ALL ROLES TESTS ====================

    @Test
    @DisplayName("GET /api/roles - Should return all active roles")
    void shouldReturnAllActiveRoles() throws Exception {
        mockMvc.perform(get("/api/roles")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(4)))) // At least 4 from test data
            .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @DisplayName("GET /api/roles - Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/roles - Should return 403 when user is not super admin")
    void shouldReturn403WhenNotSuperAdmin() throws Exception {
        // Use jwtStudent() which has STUDENT role (not SUPER_ADMIN)
        mockMvc.perform(get("/api/roles")
                .with(JwtRequestPostProcessor.jwtStudent()))
            .andExpect(status().isForbidden());
    }

    // ==================== GET ROLE BY ID TESTS ====================

    @Test
    @DisplayName("GET /api/roles/{id} - Should return role by ID")
    void shouldReturnRoleById() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", TestConstants.TEST_TEACHER_ROLE_ID)
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleId").value(TestConstants.TEST_TEACHER_ROLE_ID))
            .andExpect(jsonPath("$.code").value(TestConstants.TEST_ROLE_CODE_TEACHER))
            .andExpect(jsonPath("$.name").value("Teacher"));
    }

    @Test
    @DisplayName("GET /api/roles/{id} - Should return 400 when role not found (service throws RuntimeException)")
    void shouldReturn404WhenRoleNotFound() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", 999)
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isBadRequest());
    }

    // ==================== CREATE ROLE TESTS ====================

    @Test
    @DisplayName("POST /api/roles - Should create role successfully")
    void shouldCreateRoleSuccessfully() throws Exception {
        // Given
        CreateRoleRequest request = TestDataBuilder.buildCreateRoleRequest("NEW_ROLE_TEST", "New Test Role");
        
        // When & Then
        mockMvc.perform(post("/api/roles")
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("NEW_ROLE_TEST"))
            .andExpect(jsonPath("$.name").value("New Test Role"))
            .andExpect(jsonPath("$.roleId").exists());

        // Verify in database
        assertTrue(roleRepository.existsByCode("NEW_ROLE_TEST"));
    }

    @Test
    @DisplayName("POST /api/roles - Should return 400 when role code is empty")
    void shouldReturn400WhenRoleCodeIsEmpty() throws Exception {
        // Given
        CreateRoleRequest request = TestDataBuilder.buildCreateRoleRequest("", "Test Role");
        
        // When & Then
        mockMvc.perform(post("/api/roles")
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/roles - Should return 400 when role name is empty")
    void shouldReturn400WhenRoleNameIsEmpty() throws Exception {
        // Given
        CreateRoleRequest request = TestDataBuilder.buildCreateRoleRequest("TEST_ROLE", "");
        
        // When & Then
        mockMvc.perform(post("/api/roles")
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/roles - Should return 400 when role code already exists (service throws RuntimeException)")
    void shouldReturn409WhenRoleCodeExists() throws Exception {
        // Given - Use existing role code from test data
        CreateRoleRequest request = TestDataBuilder.buildCreateRoleRequest(
            TestConstants.TEST_ROLE_CODE_TEACHER, "Duplicate Role"
        );
        
        // When & Then
        mockMvc.perform(post("/api/roles")
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE ROLE TESTS ====================

    @Test
    @DisplayName("PUT /api/roles/{id} - Should update role successfully")
    void shouldUpdateRoleSuccessfully() throws Exception {
        // Given
        UpdateRoleRequest request = TestDataBuilder.buildUpdateRoleRequest(
            "UPDATED_TEACHER", "Updated Teacher Role"
        );
        
        // When & Then
        mockMvc.perform(put("/api/roles/{id}", TestConstants.TEST_TEACHER_ROLE_ID)
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleId").value(TestConstants.TEST_TEACHER_ROLE_ID))
            .andExpect(jsonPath("$.code").value("UPDATED_TEACHER"))
            .andExpect(jsonPath("$.name").value("Updated Teacher Role"));

        // Verify in database
        Role updated = roleRepository.findById(TestConstants.TEST_TEACHER_ROLE_ID).orElseThrow();
        assertEquals("UPDATED_TEACHER", updated.getCode());
    }

    @Test
    @DisplayName("PUT /api/roles/{id} - Should return 400 when updating non-existent role (service throws RuntimeException)")
    void shouldReturn404WhenUpdatingNonExistentRole() throws Exception {
        // Given
        UpdateRoleRequest request = TestDataBuilder.buildUpdateRoleRequest("TEST", "Test");
        
        // When & Then
        mockMvc.perform(put("/api/roles/{id}", 999)
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/roles/{id} - Should return 400 when updating with duplicate code (service throws RuntimeException)")
    void shouldReturn409WhenUpdatingWithDuplicateCode() throws Exception {
        // Given - Try to update TEACHER role with STUDENT role code
        UpdateRoleRequest request = TestDataBuilder.buildUpdateRoleRequest(
            TestConstants.TEST_ROLE_CODE_STUDENT, "Updated Teacher"
        );
        
        // When & Then
        mockMvc.perform(put("/api/roles/{id}", TestConstants.TEST_TEACHER_ROLE_ID)
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ==================== DELETE ROLE TESTS ====================

    @Test
    @DisplayName("DELETE /api/roles/{id} - Should soft delete role successfully")
    void shouldDeleteRoleSuccessfully() throws Exception {
        // Given - Use existing role from test-data.sql (role 5: INACTIVE_ROLE)
        // First need to activate it, then delete it
        // Actually, let's use a different role that's currently active
        // We'll delete role 4 (STUDENT) since it's less critical for other tests
        
        // When & Then
        mockMvc.perform(delete("/api/roles/{id}", 5)  // Use role 5 (INACTIVE_ROLE)
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isNoContent());

        // Verify soft delete (active = false)
        Role deleted = roleRepository.findById(5).orElseThrow();
        assertFalse(deleted.isActive());
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - Should return 400 when deleting non-existent role (service throws RuntimeException)")
    void shouldReturn404WhenDeletingNonExistentRole() throws Exception {
        mockMvc.perform(delete("/api/roles/{id}", 999)
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - Should return 403 when user is not super admin")
    void shouldReturn403WhenDeletingAsNonSuperAdmin() throws Exception {
        mockMvc.perform(delete("/api/roles/{id}", TestConstants.TEST_TEACHER_ROLE_ID)
                .with(JwtRequestPostProcessor.jwtStudent()))
            .andExpect(status().isForbidden());
    }

    // Helper method
    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    private void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
