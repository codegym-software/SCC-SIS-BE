package com.example.sis.controllers;

import com.example.sis.services.LessonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LessonController Unit Tests")
class LessonControllerTest {

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private LessonController lessonController;

    @Test
    @DisplayName("Should verify controller exists")
    void shouldVerifyControllerExists() {
        // THEN
        assertNotNull(lessonController);
    }

    private void assertNotNull(Object obj) {
        if (obj == null) throw new AssertionError("Object is null");
    }
}
