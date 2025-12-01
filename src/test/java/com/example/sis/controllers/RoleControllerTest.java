package com.example.sis.controllers;

import com.example.sis.services.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Unit Tests")
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    @Test
    @DisplayName("Should call service for listing roles")
    void shouldCallServiceForListingRoles() {
        // WHEN
        roleController.listRoles(null);

        // THEN
        verify(roleService, times(1)).listRolesNew(null);
    }

    @Test
    @DisplayName("Should call service for getting role by id")
    void shouldCallServiceForGettingRoleById() {
        // WHEN
        roleController.getRoleById(1);

        // THEN
        verify(roleService, times(1)).getRoleById(1);
    }
}
