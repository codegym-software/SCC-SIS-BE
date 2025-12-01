package com.example.sis.services.impl;

import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.enums.OverallStatus;
import com.example.sis.models.Enrollment;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatusManagementService Unit Tests")
class StatusManagementServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatusManagementServiceImpl statusManagementService;

    private Enrollment testEnrollment;
    private Student testStudent;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("admin@example.com");

        testStudent = new Student();
        testStudent.setStudentId(1);
        testStudent.setFullName("Test Student");
        testStudent.setOverallStatus(OverallStatus.ACTIVE);

        testEnrollment = new Enrollment();
        testEnrollment.setEnrollmentId(1);
        testEnrollment.setStudent(testStudent);
        testEnrollment.setStatus(EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should change enrollment status")
    void shouldChangeEnrollmentStatus() {
        // GIVEN
        when(enrollmentRepository.findById(1)).thenReturn(Optional.of(testEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);
        when(enrollmentRepository.findByStudent_StudentIdAndRevokedAtIsNull(1)).thenReturn(java.util.Arrays.asList(testEnrollment));
        when(studentRepository.findById(1)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        // WHEN
        statusManagementService.changeEnrollmentStatus(1, EnrollmentStatus.DROPPED, 1, "Test note");

        // THEN
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Should change student status")
    void shouldChangeStudentStatus() {
        // GIVEN
        when(studentRepository.findById(1)).thenReturn(Optional.of(testStudent));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        // WHEN
        statusManagementService.changeStudentStatus(1, OverallStatus.DROPPED, 1, "Test note");

        // THEN
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("Should throw exception when enrollment not found")
    void shouldThrowExceptionWhenEnrollmentNotFound() {
        // GIVEN
        when(enrollmentRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> 
            statusManagementService.changeEnrollmentStatus(999, EnrollmentStatus.ACTIVE, 1, null));
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowExceptionWhenStudentNotFound() {
        // GIVEN
        when(studentRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> 
            statusManagementService.changeStudentStatus(999, OverallStatus.ACTIVE, 1, null));
    }

    @Test
    @DisplayName("Should not allow changing enrollment when student is DROPPED")
    void shouldNotAllowChangingEnrollmentWhenStudentDropped() {
        // GIVEN
        testStudent.setOverallStatus(OverallStatus.DROPPED);
        when(enrollmentRepository.findById(1)).thenReturn(Optional.of(testEnrollment));

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> 
            statusManagementService.changeEnrollmentStatus(1, EnrollmentStatus.ACTIVE, 1, null));
    }
}
