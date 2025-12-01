package com.example.sis.services.impl;

import com.example.sis.models.ClassEntity;
import com.example.sis.models.Enrollment;
import com.example.sis.models.Student;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.services.NotificationService;
import com.example.sis.services.StatusManagementService;
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
@DisplayName("EnrollmentService Unit Tests")
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private ClassRepository classRepo;

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private EntityManager em;

    @Mock
    private StatusManagementService statusManagementService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Enrollment testEnrollment;
    private ClassEntity testClass;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testClass = new ClassEntity();
        testClass.setClassId(1);
        testClass.setName("Test Class");

        testStudent = new Student();
        testStudent.setStudentId(1);
        testStudent.setFullName("Test Student");

        testEnrollment = new Enrollment();
        testEnrollment.setEnrollmentId(1);
        testEnrollment.setClassEntity(testClass);
        testEnrollment.setStudent(testStudent);
    }

    @Test
    @DisplayName("Should find enrollment by id")
    void shouldFindEnrollmentById() {
        // GIVEN
        when(enrollmentRepo.findById(1)).thenReturn(Optional.of(testEnrollment));

        // WHEN
        Optional<Enrollment> result = enrollmentRepo.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getEnrollmentId());
        verify(enrollmentRepo, times(1)).findById(1);
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
    }

    @Test
    @DisplayName("Should find student by id")
    void shouldFindStudentById() {
        // GIVEN
        when(studentRepo.findById(1)).thenReturn(Optional.of(testStudent));

        // WHEN
        Optional<Student> result = studentRepo.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test Student", result.get().getFullName());
    }
}
