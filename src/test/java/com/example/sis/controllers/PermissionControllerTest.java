package com.example.sis.controllers;

import com.example.sis.util.JwtRequestPostProcessor;
import com.example.sis.util.TestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PermissionController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/test-data.sql")
@DisplayName("PermissionController Integration Tests")
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET PERMISSION GROUPS TESTS ====================

    @Test
    @DisplayName("GET /api/permissions/groups - Should return all permission groups")
    void shouldReturnAllPermissionGroups() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1)))) // At least 1 group
            .andExpect(jsonPath("$[0].category").exists())
            .andExpect(jsonPath("$[0].items").isArray())
            .andExpect(jsonPath("$[0].total").isNumber());
    }

    @Test
    @DisplayName("GET /api/permissions/groups - Should filter by category")
    void shouldFilterPermissionsByCategory() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .param("category", "STUDENT")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].category", everyItem(is("STUDENT"))));
    }

    @Test
    @DisplayName("GET /api/permissions/groups - Should search by query")
    void shouldSearchPermissionsByQuery() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .param("q", "READ")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/permissions/groups - Should filter by active status")
    void shouldFilterByActiveStatus() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .param("activeOnly", "true")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/permissions/groups - Should include granted status for role")
    void shouldIncludeGrantedStatusForRole() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .param("roleId", String.valueOf(TestConstants.TEST_TEACHER_ROLE_ID))
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].items[0].granted").exists());
    }

    @Test
    @DisplayName("GET /api/permissions/groups - Should include empty groups when requested")
    void shouldIncludeEmptyGroups() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .param("includeEmpty", "true")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ==================== GET PERMISSION BY ID TESTS (DEPRECATED) ====================

    @Test
    @DisplayName("GET /api/permissions/{id} - Should return permission by ID")
    void shouldReturnPermissionById() throws Exception {
        mockMvc.perform(get("/api/permissions/{id}", TestConstants.TEST_PERMISSION_READ_STUDENT)
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissionId").value(TestConstants.TEST_PERMISSION_READ_STUDENT))
            .andExpect(jsonPath("$.code").exists())
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.category").exists());
    }

    @Test
    @DisplayName("GET /api/permissions/{id} - Should return 404 when permission not found")
    void shouldReturn404WhenPermissionNotFound() throws Exception {
        mockMvc.perform(get("/api/permissions/{id}", 999)
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isNotFound());
    }

    // ==================== LIST ALL PERMISSIONS TESTS (DEPRECATED) ====================

    @Test
    @DisplayName("GET /api/permissions - Should return all permissions with pagination")
    void shouldReturnAllPermissionsWithPagination() throws Exception {
        mockMvc.perform(get("/api/permissions")
                .param("page", "0")
                .param("size", "10")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/permissions - Should filter by active status")
    void shouldFilterListByActiveStatus() throws Exception {
        mockMvc.perform(get("/api/permissions")
                .param("active", "true")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/permissions - Should search by query")
    void shouldSearchPermissionsListByQuery() throws Exception {
        mockMvc.perform(get("/api/permissions")
                .param("q", "STUDENT")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ==================== GET CATEGORIES TESTS (DEPRECATED) ====================

    @Test
    @DisplayName("GET /api/permissions/categories - Should return all categories")
    void shouldReturnAllCategories() throws Exception {
        mockMvc.perform(get("/api/permissions/categories")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @DisplayName("GET /api/permissions/groups - Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/permissions/groups"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/permissions/groups - Should allow SUPER_ADMIN")
    void shouldAllowSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/permissions/groups")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/permissions - Should return 401 when not authenticated")
    void shouldReturn401WhenListingWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/permissions"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/permissions/categories - Should return 401 when not authenticated")
    void shouldReturn401WhenGettingCategoriesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/permissions/categories"))
            .andExpect(status().isUnauthorized());
    }
}
