package com.example.sis.services.impl;

import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.Enrollment;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentClassService Unit Tests")
class StudentClassServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentClassServiceImpl studentClassService;

    private ClassEntity testClass;
    private Enrollment testEnrollment;

    @BeforeEach
    void setUp() {
        testClass = new ClassEntity();
        testClass.setClassId(1);
        testClass.setName("Test Class");

        testEnrollment = new Enrollment();
        testEnrollment.setEnrollmentId(1);
    }

    @Test
    @DisplayName("Should get classes by student id")
    void shouldGetClassesByStudentId() {
        // GIVEN
        when(studentRepository.existsById(1)).thenReturn(true);
        when(classRepository.findAll()).thenReturn(Arrays.asList(testClass));
        when(enrollmentRepository.findActiveByClassAndStudent(eq(1), eq(1), any()))
                .thenReturn(Arrays.asList(testEnrollment));

        // WHEN
        List<ClassResponse> result = studentClassService.getClassesByStudentId(1);

        // THEN
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(studentRepository, times(1)).existsById(1);
        verify(classRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when student has no classes")
    void shouldReturnEmptyListWhenNoClasses() {
        // GIVEN
        when(studentRepository.existsById(1)).thenReturn(true);
        when(classRepository.findAll()).thenReturn(Arrays.asList(testClass));
        when(enrollmentRepository.findActiveByClassAndStudent(anyInt(), anyInt(), any()))
                .thenReturn(Arrays.asList());

        // WHEN
        List<ClassResponse> result = studentClassService.getClassesByStudentId(1);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowExceptionWhenStudentNotFound() {
        // GIVEN
        when(studentRepository.existsById(999)).thenReturn(false);

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> studentClassService.getClassesByStudentId(999));
    }
}
