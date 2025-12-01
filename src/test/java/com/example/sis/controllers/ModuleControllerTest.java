package com.example.sis.controllers;

import com.example.sis.services.ModuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleController Unit Tests")
class ModuleControllerTest {

    @Mock
    private ModuleService moduleService;

    @InjectMocks
    private ModuleController moduleController;

    @Test
    @DisplayName("Should call service for getting modules by program")
    void shouldCallServiceForGettingModulesByProgram() {
        // WHEN
        moduleController.getModulesByProgram(1, null, null, null, null);

        // THEN
        verify(moduleService, times(1)).getModulesByProgramId(1);
    }
}
