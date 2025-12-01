package com.example.sis.unittest;

import com.example.sis.controllers.CenterController;
import com.example.sis.dtos.center.CenterLiteResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.dtos.center.CreateCenterRequest;
import com.example.sis.dtos.center.UpdateCenterRequest;
import com.example.sis.models.enums.Status;
import com.example.sis.services.CenterService;
import com.example.sis.util.JwtRequestPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for CenterController
 * Using @WebMvcTest to test only the web layer with mocked service
 */
@WebMvcTest(CenterController.class)
@DisplayName("CenterController Unit Tests")
class CenterControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CenterService centerService;

    @Autowired
    private ObjectMapper objectMapper;

    private CenterResponse centerResponse;
    private CenterLiteResponse centerLiteResponse;
    private List<CenterResponse> centerList;
    private List<CenterLiteResponse> centerLiteList;

    @BeforeEach
    void setUp() {
        // Prepare test data
        centerResponse = new CenterResponse(
            1,
            "HN01",
            "Hà Nội Center",
            "123 Láng Hạ, Đống Đa",
            "024-12345678",
            "hanoi@codegym.vn",
            Status.ACTIVE,
            LocalDateTime.now(),
            LocalDateTime.now(),
            1,
            1
        );

        centerLiteResponse = new CenterLiteResponse(1, "HN01", "Hà Nội Center");

        CenterResponse center2 = new CenterResponse(
            2, "HCM01", "HCM Center", "456 Nguyễn Văn Cừ",
            "028-87654321", "hcm@codegym.vn", Status.ACTIVE,
            LocalDateTime.now(), LocalDateTime.now(), 1, 1
        );

        centerList = Arrays.asList(centerResponse, center2);
        centerLiteList = Arrays.asList(
            centerLiteResponse,
            new CenterLiteResponse(2, "HCM01", "HCM Center")
        );
    }

    // ==================== GET /api/centers/lite ====================

    @Test
    @DisplayName("GET /api/centers/lite - Should return lite centers list")
    void testGetCentersLite_ShouldReturnLiteList() throws Exception {
        // Given
        given(centerService.listCentersLite()).willReturn(centerLiteList);

        // When & Then
        mockMvc.perform(get("/api/centers/lite"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].centerId", is(1)))
            .andExpect(jsonPath("$[0].centerCode", is("HN01")))
            .andExpect(jsonPath("$[0].centerName", is("Hà Nội Center")))
            .andExpect(jsonPath("$[1].centerId", is(2)));

        verify(centerService, times(1)).listCentersLite();
    }

    // ==================== GET /api/centers ====================

    @Test
    @DisplayName("GET /api/centers - Should return all active centers when authenticated as SuperAdmin")
    void testGetAllActiveCenters_AsSuperAdmin_ShouldReturnList() throws Exception {
        // Given
        given(centerService.getAllActiveCenters()).willReturn(centerList);

        // When & Then
        mockMvc.perform(get("/api/centers")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].centerId", is(1)))
            .andExpect(jsonPath("$[0].centerCode", is("HN01")));

        verify(centerService, times(1)).getAllActiveCenters();
    }

    @Test
    @DisplayName("GET /api/centers - Should return 401 when not authenticated")
    void testGetAllActiveCenters_NotAuthenticated_ShouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/centers"))
            .andExpect(status().isUnauthorized());

        verify(centerService, never()).getAllActiveCenters();
    }

    @Test
    @DisplayName("GET /api/centers - Should return 403 when not SuperAdmin")
    void testGetAllActiveCenters_NotSuperAdmin_ShouldReturn403() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/centers")
                .with(JwtRequestPostProcessor.jwtStudent()))
            .andExpect(status().isForbidden());

        verify(centerService, never()).getAllActiveCenters();
    }

    // ==================== GET /api/centers/all ====================

    @Test
    @DisplayName("GET /api/centers/all - Should return all centers including deactivated")
    void testGetAllCenters_AsSuperAdmin_ShouldReturnAllCenters() throws Exception {
        // Given
        given(centerService.getAllCenters()).willReturn(centerList);

        // When & Then
        mockMvc.perform(get("/api/centers/all")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(centerService, times(1)).getAllCenters();
    }

    // ==================== GET /api/centers/{id} ====================

    @Test
    @DisplayName("GET /api/centers/{id} - Should return center by ID")
    void testGetCenterById_WhenExists_ShouldReturnCenter() throws Exception {
        // Given
        given(centerService.getCenterById(1)).willReturn(centerResponse);

        // When & Then
        mockMvc.perform(get("/api/centers/1")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.centerId", is(1)))
            .andExpect(jsonPath("$.centerCode", is("HN01")))
            .andExpect(jsonPath("$.centerName", is("Hà Nội Center")))
            .andExpect(jsonPath("$.address", is("123 Láng Hạ, Đống Đa")))
            .andExpect(jsonPath("$.phoneNumber", is("024-12345678")))
            .andExpect(jsonPath("$.email", is("hanoi@codegym.vn")));

        verify(centerService, times(1)).getCenterById(1);
    }

    // ==================== POST /api/centers ====================

    @Test
    @DisplayName("POST /api/centers - Should create center with valid data")
    void testCreateCenter_WithValidData_ShouldReturnCreated() throws Exception {
        // Given
        CreateCenterRequest request = new CreateCenterRequest();
        request.setCenterCode("DN01");
        request.setCenterName("Đà Nẵng Center");
        request.setAddress("789 Nguyễn Văn Linh");
        request.setPhoneNumber("0236-3456789");
        request.setEmail("danang@codegym.vn");

        CenterResponse createdCenter = new CenterResponse(
            3, "DN01", "Đà Nẵng Center", "789 Nguyễn Văn Linh",
            "0236-3456789", "danang@codegym.vn", Status.ACTIVE,
            LocalDateTime.now(), LocalDateTime.now(), 1, 1
        );

        given(centerService.createCenter(any(CreateCenterRequest.class), any(Integer.class)))
            .willReturn(createdCenter);

        // When & Then
        mockMvc.perform(post("/api/centers")
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.centerId", is(3)))
            .andExpect(jsonPath("$.centerCode", is("DN01")))
            .andExpect(jsonPath("$.centerName", is("Đà Nẵng Center")));

        verify(centerService, times(1)).createCenter(any(CreateCenterRequest.class), any(Integer.class));
    }

    @Test
    @DisplayName("POST /api/centers - Should return 401 when not authenticated")
    void testCreateCenter_NotAuthenticated_ShouldReturn401() throws Exception {
        // Given
        CreateCenterRequest request = new CreateCenterRequest();
        request.setCenterCode("DN01");

        // When & Then
        mockMvc.perform(post("/api/centers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(centerService, never()).createCenter(any(), any());
    }

    // ==================== PUT /api/centers/{id} ====================

    @Test
    @DisplayName("PUT /api/centers/{id} - Should update center")
    void testUpdateCenter_WithValidData_ShouldReturnUpdated() throws Exception {
        // Given
        UpdateCenterRequest request = new UpdateCenterRequest();
        request.setCenterName("Hà Nội Center Updated");
        request.setAddress("123 Láng Hạ Updated");

        CenterResponse updatedCenter = new CenterResponse(
            1, "HN01", "Hà Nội Center Updated", "123 Láng Hạ Updated",
            "024-12345678", "hanoi@codegym.vn", Status.ACTIVE,
            LocalDateTime.now(), LocalDateTime.now(), 1, 1
        );

        given(centerService.updateCenter(eq(1), any(UpdateCenterRequest.class), any(Integer.class)))
            .willReturn(updatedCenter);

        // When & Then
        mockMvc.perform(put("/api/centers/1")
                .with(JwtRequestPostProcessor.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.centerId", is(1)))
            .andExpect(jsonPath("$.centerName", is("Hà Nội Center Updated")));

        verify(centerService, times(1)).updateCenter(eq(1), any(UpdateCenterRequest.class), any(Integer.class));
    }

    // ==================== DELETE /api/centers/{id} ====================

    @Test
    @DisplayName("DELETE /api/centers/{id} - Should deactivate center")
    void testDeactivateCenter_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(centerService).deactivateCenter(eq(1), any(Integer.class));

        // When & Then
        mockMvc.perform(delete("/api/centers/1")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isNoContent());

        verify(centerService, times(1)).deactivateCenter(eq(1), any(Integer.class));
    }

    // ==================== PUT /api/centers/{id}/reactivate ====================

    @Test
    @DisplayName("PUT /api/centers/{id}/reactivate - Should reactivate center")
    void testReactivateCenter_ShouldReturnReactivatedCenter() throws Exception {
        // Given
        doNothing().when(centerService).reactivateCenter(eq(1), any(Integer.class));
        given(centerService.getCenterById(1)).willReturn(centerResponse);

        // When & Then
        mockMvc.perform(put("/api/centers/1/reactivate")
                .with(JwtRequestPostProcessor.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.centerId", is(1)))
            .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(centerService, times(1)).reactivateCenter(eq(1), any(Integer.class));
        verify(centerService, times(1)).getCenterById(1);
    }
}
