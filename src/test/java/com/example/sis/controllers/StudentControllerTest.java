package com.example.sis.controllers;

import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentController Unit Tests")
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private StudentController studentController;

    @Test
    @DisplayName("Should call service for getting all students")
    void shouldCallServiceForGettingAllStudents() {
        // WHEN
        studentController.getAllStudents();

        // THEN
        verify(studentService, times(1)).getAllStudents();
    }
}
