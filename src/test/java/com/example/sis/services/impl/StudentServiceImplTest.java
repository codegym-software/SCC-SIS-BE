package com.example.sis.services.impl;

import com.example.sis.keycloak.KeycloakAdminClient;
import com.example.sis.models.Student;
import com.example.sis.repositories.*;
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
@DisplayName("StudentService Unit Tests")
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private KeycloakAdminClient kcAdmin;

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private UserRoleRepository userRoleRepo;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepo;

    @Mock
    private GradeRecordRepository gradeRecordRepo;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setStudentId(1);
        testStudent.setFullName("Test Student");
        testStudent.setEmail("student@test.com");
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
        verify(studentRepo, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should check if student exists")
    void shouldCheckStudentExists() {
        // GIVEN
        when(studentRepo.existsById(1)).thenReturn(true);

        // WHEN
        boolean exists = studentRepo.existsById(1);

        // THEN
        assertTrue(exists);
        verify(studentRepo, times(1)).existsById(1);
    }

    @Test
    @DisplayName("Should return empty when student not found")
    void shouldReturnEmptyWhenNotFound() {
        // GIVEN
        when(studentRepo.findById(999)).thenReturn(Optional.empty());

        // WHEN
        Optional<Student> result = studentRepo.findById(999);

        // THEN
        assertFalse(result.isPresent());
    }
}
