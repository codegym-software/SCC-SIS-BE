package com.example.sis.services.impl;

import com.example.sis.dtos.center.CenterLiteResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.dtos.center.CreateCenterRequest;
import com.example.sis.dtos.center.UpdateCenterRequest;
import com.example.sis.models.Center;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CenterService Unit Tests")
class CenterServiceImplTest {

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CenterServiceImpl centerService;

    private Center testCenter;
    private CenterLiteResponse liteResponse;

    @BeforeEach
    void setUp() {
        testCenter = new Center();
        testCenter.setCenterId(1);
        testCenter.setName("Test Center");
        testCenter.setCode("TC01");
        testCenter.setPhone("0123456789");
        testCenter.setEmail("test@center.com");

        liteResponse = new CenterLiteResponse(1, "TC01", "Test Center");
    }

    @Test
    @DisplayName("Should get all centers lite")
    void shouldGetAllCentersLite() {
        // GIVEN
        when(centerRepository.findLiteActive()).thenReturn(Arrays.asList(liteResponse));

        // WHEN
        List<CenterLiteResponse> result = centerService.listCentersLite();

        // THEN
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Center", result.get(0).getName());
        verify(centerRepository, times(1)).findLiteActive();
    }

    @Test
    @DisplayName("Should get all active centers")
    void shouldGetAllActiveCenters() {
        // GIVEN
        when(centerRepository.findAllActiveOrderByCreatedAtDesc()).thenReturn(Arrays.asList(testCenter));

        // WHEN
        List<CenterResponse> result = centerService.getAllActiveCenters();

        // THEN
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(centerRepository, times(1)).findAllActiveOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should get center by id")
    void shouldGetCenterById() {
        // GIVEN
        when(centerRepository.findActiveById(1)).thenReturn(Optional.of(testCenter));

        // WHEN
        CenterResponse result = centerService.getCenterById(1);

        // THEN
        assertNotNull(result);
        assertEquals("Test Center", result.getName());
        assertEquals("TC01", result.getCode());
        verify(centerRepository, times(1)).findActiveById(1);
    }

    @Test
    @DisplayName("Should create center successfully")
    void shouldCreateCenter() {
        // GIVEN
        CreateCenterRequest request = new CreateCenterRequest();
        request.setName("New Center");
        request.setCode("NC01");
        request.setEmail("new@center.com");
        request.setPhone("0987654321");
        request.setAddressLine("123 Street");
        request.setProvince("HN");
        request.setDistrict("District");
        request.setWard("Ward");

        when(centerRepository.save(any(Center.class))).thenReturn(testCenter);
        doNothing().when(notificationService).notifyAdminsExcept(anyInt(), any(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());

        // WHEN
        CenterResponse result = centerService.createCenter(request, 1);

        // THEN
        assertNotNull(result);
        verify(centerRepository, times(1)).save(any(Center.class));
    }

    @Test
    @DisplayName("Should update center successfully")
    void shouldUpdateCenter() {
        // GIVEN
        UpdateCenterRequest request = new UpdateCenterRequest();
        request.setName("Updated Center");

        when(centerRepository.findActiveById(1)).thenReturn(Optional.of(testCenter));
        when(centerRepository.save(any(Center.class))).thenReturn(testCenter);

        // WHEN
        CenterResponse result = centerService.updateCenter(1, request, 1);

        // THEN
        assertNotNull(result);
        verify(centerRepository, times(1)).save(any(Center.class));
    }

    @Test
    @DisplayName("Should throw exception when center not found")
    void shouldThrowExceptionWhenCenterNotFound() {
        // GIVEN
        when(centerRepository.findActiveById(999)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(Exception.class, () -> centerService.getCenterById(999));
    }
}
