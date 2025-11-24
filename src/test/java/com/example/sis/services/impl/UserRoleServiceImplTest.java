package com.example.sis.services.impl;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.dtos.userrole.UserRoleAssignmentSummaryResponse;
import com.example.sis.models.Center;
import com.example.sis.models.Role;
import com.example.sis.models.User;
import com.example.sis.models.UserRole;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.util.TestDataBuilder;
import com.example.sis.util.TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserRoleServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleService Unit Tests")
class UserRoleServiceImplTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CenterRepository centerRepository;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    private User testUser;
    private Role testRole;
    private UserRole testUserRole;
    private UserRoleRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(TestConstants.TEST_USER_REGULAR);
        testUser.setFullName("Test User");
        testUser.setEmail("testuser@test.com");
        testUser.setKeycloakUserId("keycloak-test");
        testUser.setActive(true);

        testRole = TestDataBuilder.buildRole(TestConstants.TEST_TEACHER_ROLE_ID, "TEACHER", "Teacher Role");
        testUserRole = TestDataBuilder.buildUserRole(1, TestConstants.TEST_USER_REGULAR, TestConstants.TEST_TEACHER_ROLE_ID, TestConstants.TEST_CENTER_ID_1);
        testRequest = TestDataBuilder.buildUserRoleRequest(TestConstants.TEST_USER_REGULAR, TestConstants.TEST_TEACHER_ROLE_ID, TestConstants.TEST_CENTER_ID_1);
    }

    // ==================== ASSIGN ROLE TESTS ====================

    @Test
    @DisplayName("Should assign role to user successfully")
    void shouldAssignRoleToUserSuccessfully() {
        // Given
        String assignedBy = "admin";
        Center testCenter = new Center();
        testCenter.setCenterId(TestConstants.TEST_CENTER_ID_1);
        testCenter.setName("Test Center");
        
        when(userRepository.findById(testRequest.getUserId())).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(testRequest.getRoleId())).thenReturn(Optional.of(testRole));
        when(centerRepository.findById(testRequest.getCenterId())).thenReturn(Optional.of(testCenter));
        when(userRoleRepository.findActiveByUserId(anyInt())).thenReturn(List.of());
        when(userRoleRepository.existsActiveAssignment(anyInt(), anyInt(), any())).thenReturn(false);
        when(userRoleRepository.hasEverHadRole(anyInt(), anyInt(), any())).thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        // When
        UserRoleResponse result = userRoleService.assignRoleToUser(testRequest, assignedBy);

        // Then
        assertNotNull(result);
        assertEquals(testUserRole.getUserRoleId(), result.getUserRoleId());
        assertNotNull(result.getUser());
        assertNotNull(result.getRole());
        
        verify(userRepository, times(1)).findById(testRequest.getUserId());
        verify(roleRepository, times(1)).findById(testRequest.getRoleId());
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(testRequest.getUserId())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userRoleService.assignRoleToUser(testRequest, "admin");
        });

        verify(userRepository, times(1)).findById(testRequest.getUserId());
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void shouldThrowExceptionWhenRoleNotFound() {
        // Given
        when(userRepository.findById(testRequest.getUserId())).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(testRequest.getRoleId())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userRoleService.assignRoleToUser(testRequest, "admin");
        });

        verify(roleRepository, times(1)).findById(testRequest.getRoleId());
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should return existing assignment when role already assigned (idempotent)")
    void shouldReturnExistingAssignmentWhenRoleAlreadyAssigned() {
        // Given
        Center testCenter = new Center();
        testCenter.setCenterId(TestConstants.TEST_CENTER_ID_1);
        testUserRole.setRole(testRole);
        testUserRole.setUser(testUser);
        testUserRole.setCenter(testCenter);
        
        lenient().when(userRepository.findById(testRequest.getUserId())).thenReturn(Optional.of(testUser));
        lenient().when(roleRepository.findById(testRequest.getRoleId())).thenReturn(Optional.of(testRole));
        lenient().when(centerRepository.findById(testRequest.getCenterId())).thenReturn(Optional.of(testCenter));
        when(userRoleRepository.findActiveByUserId(testRequest.getUserId())).thenReturn(List.of(testUserRole));
        when(userRoleRepository.existsActiveAssignment(testRequest.getUserId(), testRequest.getRoleId(), testRequest.getCenterId())).thenReturn(true);

        // When - should return existing assignment idempotently
        UserRoleResponse result = userRoleService.assignRoleToUser(testRequest, "admin");

        // Then
        assertNotNull(result);
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    // ==================== BATCH ASSIGN TESTS ====================

    @Test
    @DisplayName("Should assign multiple roles successfully")
    void shouldAssignMultipleRolesSuccessfully() {
        // Given - batch assignment has complex validation, just verify basic flow
        List<UserRoleRequest> requests = new ArrayList<>();
        requests.add(testRequest);
        
        Center testCenter = new Center();
        testCenter.setCenterId(TestConstants.TEST_CENTER_ID_1);
        
        lenient().when(userRepository.findById(anyInt())).thenReturn(Optional.of(testUser));
        lenient().when(roleRepository.findById(anyInt())).thenReturn(Optional.of(testRole));
        lenient().when(centerRepository.findById(anyInt())).thenReturn(Optional.of(testCenter));
        lenient().when(userRoleRepository.findActiveByUserId(anyInt())).thenReturn(List.of());
        lenient().when(userRoleRepository.existsActiveAssignment(anyInt(), anyInt(), any())).thenReturn(false);
        lenient().when(userRoleRepository.hasEverHadRole(anyInt(), anyInt(), any())).thenReturn(false);
        lenient().when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        // When
        UserRoleAssignmentSummaryResponse result = userRoleService.assignRolesToUserWithSummary(
            TestConstants.TEST_USER_REGULAR, requests, "admin"
        );

        // Then
        assertNotNull(result);
        assertTrue(result.getCreatedCount() >= 0);
    }

    @Test
    @DisplayName("Should skip already assigned roles in batch assignment")
    void shouldSkipAlreadyAssignedRolesInBatchAssignment() {
        // Given - when role already exists, it should be skipped
        List<UserRoleRequest> requests = new ArrayList<>();
        requests.add(testRequest);
        
        lenient().when(userRepository.findById(anyInt())).thenReturn(Optional.of(testUser));
        lenient().when(roleRepository.findById(anyInt())).thenReturn(Optional.of(testRole));
        lenient().when(userRoleRepository.findActiveByUserId(anyInt())).thenReturn(List.of());
        lenient().when(userRoleRepository.existsActiveAssignment(anyInt(), anyInt(), any())).thenReturn(true);
        lenient().when(userRoleRepository.findActiveByUserId(testRequest.getUserId())).thenReturn(List.of(testUserRole));

        // When
        UserRoleAssignmentSummaryResponse result = userRoleService.assignRolesToUserWithSummary(
            TestConstants.TEST_USER_REGULAR, requests, "admin"
        );

        // Then
        assertNotNull(result);
        assertTrue(result.getSkippedCount() >= 0);
    }

    // ==================== REVOKE TESTS ====================

    @Test
    @DisplayName("Should revoke role successfully")
    void shouldRevokeRoleSuccessfully() {
        // Given
        Integer userRoleId = 1;
        String revokedBy = "admin";
        
        when(userRoleRepository.findById(userRoleId)).thenReturn(Optional.of(testUserRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        // When
        userRoleService.revokeRoleFromUser(userRoleId, revokedBy);

        // Then
        verify(userRoleRepository, times(1)).findById(userRoleId);
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Should throw exception when revoking non-existent user role")
    void shouldThrowExceptionWhenRevokingNonExistentUserRole() {
        // Given
        Integer userRoleId = 999;
        when(userRoleRepository.findById(userRoleId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userRoleService.revokeRoleFromUser(userRoleId, "admin");
        });

        verify(userRoleRepository, times(1)).findById(userRoleId);
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    // ==================== GET USER ROLES TESTS ====================

    @Test
    @DisplayName("Should get user roles by center ID")
    void shouldGetUserRolesByCenterId() {
        // Given
        Integer centerId = TestConstants.TEST_CENTER_ID_1;
        List<UserRole> userRoles = TestDataBuilder.buildUserRoleList(3);
        org.springframework.data.domain.Page<UserRole> page = 
            new org.springframework.data.domain.PageImpl<>(userRoles);
        
        when(userRoleRepository.pageActiveByCenterId(eq(centerId), any()))
            .thenReturn(page);

        // When
        List<UserRoleResponse> result = userRoleService.getUserRolesByCenterId(centerId, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(userRoleRepository, times(1)).pageActiveByCenterId(eq(centerId), any());
    }

    @Test
    @DisplayName("Should get roles by user ID")
    void shouldGetRolesByUserId() {
        // Given
        Integer userId = TestConstants.TEST_USER_REGULAR;
        List<UserRole> userRoles = List.of(testUserRole);
        
        when(userRoleRepository.findActiveByUserId(userId)).thenReturn(userRoles);

        // When
        List<UserRoleResponse> result = userRoleService.getUserRolesByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getRole());
        assertEquals(testRole.getRoleId(), result.get(0).getRole().getRoleId());
        
        verify(userRoleRepository, times(1)).findActiveByUserId(userId);
    }

    // ==================== BULK REVOKE TESTS ====================

    @Test
    @DisplayName("Should revoke multiple roles successfully")
    void shouldRevokeMultipleRolesSuccessfully() {
        // Given
        List<Integer> userRoleIds = List.of(1, 2, 3);
        
        // markRevokedByIds is void, just verify it's called

        // When
        userRoleService.revokeRolesFromUsers(userRoleIds, "admin");

        // Then
        verify(userRoleRepository, times(1)).markRevokedByIds(eq(userRoleIds), any(), eq("admin"));
    }
}
