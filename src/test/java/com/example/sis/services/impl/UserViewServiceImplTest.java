package com.example.sis.services.impl;

import com.example.sis.dtos.user.UserAssignmentRow;
import com.example.sis.dtos.user.UserViewResponse;
import com.example.sis.repositories.UserViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserViewService Unit Tests")
class UserViewServiceImplTest {

    @Mock
    private UserViewRepository userViewRepository;

    @InjectMocks
    private UserViewServiceImpl userViewService;

    @Test
    @DisplayName("Should search users and return empty list")
    void shouldSearchUsersReturnEmpty() {
        // GIVEN
        when(userViewRepository.searchUserViews(anyInt(), anyString(), anyString(), anyList()))
                .thenReturn(new ArrayList<>());

        // WHEN
        List<UserViewResponse> result = userViewService.search(1, "TEACHER", "test");

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userViewRepository, times(1)).searchUserViews(anyInt(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Should search users with null center")
    void shouldSearchUsersWithNullCenter() {
        // GIVEN
        when(userViewRepository.searchUserViews(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // WHEN
        List<UserViewResponse> result = userViewService.search(null, "SUPER_ADMIN", null);

        // THEN
        assertNotNull(result);
        verify(userViewRepository, times(1)).searchUserViews(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle search with empty query")
    void shouldHandleEmptyQuery() {
        // GIVEN
        when(userViewRepository.searchUserViews(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // WHEN
        List<UserViewResponse> result = userViewService.search(1, "TEACHER", "");

        // THEN
        assertNotNull(result);
        verify(userViewRepository, times(1)).searchUserViews(any(), any(), any(), any());
    }
}
