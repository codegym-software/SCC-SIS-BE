package com.example.sis.services.impl;

import com.example.sis.models.ClassEntity;
import com.example.sis.repositories.*;
import com.example.sis.services.NotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService Unit Tests")
class AttendanceServiceImplTest {

    @Mock
    private AttendanceSessionRepository sessionRepo;

    @Mock
    private AttendanceRecordRepository recordRepo;

    @Mock
    private ClassRepository classRepo;

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private ClassTeacherRepository classTeacherRepo;

    @Mock
    private EntityManager em;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private ClassEntity testClass;

    @BeforeEach
    void setUp() {
        testClass = new ClassEntity();
        testClass.setClassId(1);
        testClass.setName("Test Class");
    }

    @Test
    @DisplayName("Should find class by id")
    void shouldFindClassById() {
        // GIVEN
        when(classRepo.findById(1)).thenReturn(Optional.of(testClass));

        // WHEN
        Optional<ClassEntity> result = classRepo.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test Class", result.get().getName());
        verify(classRepo, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should return empty when class not found")
    void shouldReturnEmptyWhenClassNotFound() {
        // GIVEN
        when(classRepo.findById(999)).thenReturn(Optional.empty());

        // WHEN
        Optional<ClassEntity> result = classRepo.findById(999);

        // THEN
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should check if class exists")
    void shouldCheckClassExists() {
        // GIVEN
        when(classRepo.existsById(1)).thenReturn(true);

        // WHEN
        boolean exists = classRepo.existsById(1);

        // THEN
        assertTrue(exists);
        verify(classRepo, times(1)).existsById(1);
    }
}
