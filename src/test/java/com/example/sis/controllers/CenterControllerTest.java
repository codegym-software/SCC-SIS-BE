package com.example.sis.controllers;

import com.example.sis.services.CenterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CenterController Unit Tests")
class CenterControllerTest {

    @Mock
    private CenterService centerService;

    @InjectMocks
    private CenterController centerController;

    @Test
    @DisplayName("Should call service for listing centers lite")
    void shouldCallServiceForListingCentersLite() {
        // WHEN
        centerController.listCentersLite();

        // THEN
        verify(centerService, times(1)).listCentersLite();
    }

    @Test
    @DisplayName("Should call service for getting all active centers")
    void shouldCallServiceForGettingAllActiveCenters() {
        // WHEN
        centerController.getAllActiveCenters();

        // THEN
        verify(centerService, times(1)).getAllActiveCenters();
    }
}
