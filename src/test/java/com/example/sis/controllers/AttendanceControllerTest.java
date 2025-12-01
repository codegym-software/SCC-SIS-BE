package com.example.sis.controllers;

import com.example.sis.services.AttendanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceController Unit Tests")
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private AttendanceController attendanceController;

    @Test
    @DisplayName("Should verify controller exists")
    void shouldVerifyControllerExists() {
        // THEN
        assertNotNull(attendanceController);
    }

    private void assertNotNull(Object obj) {
        if (obj == null) throw new AssertionError("Object is null");
    }
}
