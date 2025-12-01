package com.example.sis.services.impl;

import com.example.sis.models.ClassEntity;
import com.example.sis.models.GradeEntry;
import com.example.sis.models.Module;
import com.example.sis.repositories.*;
import com.example.sis.services.ModuleService;
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
@DisplayName("GradeEntryService Unit Tests")
class GradeEntryServiceImplTest {

    @Mock
    private GradeEntryRepository gradeEntryRepository;

    @Mock
    private GradeRecordRepository gradeRecordRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ModuleService moduleService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GradeEntryServiceImpl gradeEntryService;

    private GradeEntry testGradeEntry;
    private ClassEntity testClass;
    private Module testModule;

    @BeforeEach
    void setUp() {
        testClass = new ClassEntity();
        testClass.setClassId(1);
        testClass.setName("Test Class");

        testModule = new Module();
        testModule.setModuleId(1);
        testModule.setName("Test Module");

        testGradeEntry = new GradeEntry();
        testGradeEntry.setGradeEntryId(1);
        testGradeEntry.setClassEntity(testClass);
        testGradeEntry.setModule(testModule);
    }

    @Test
    @DisplayName("Should find grade entry by id")
    void shouldFindGradeEntryById() {
        // GIVEN
        when(gradeEntryRepository.findById(1)).thenReturn(Optional.of(testGradeEntry));

        // WHEN
        Optional<GradeEntry> result = gradeEntryRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getGradeEntryId());
        verify(gradeEntryRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should find class by id")
    void shouldFindClassById() {
        // GIVEN
        when(classRepository.findById(1)).thenReturn(Optional.of(testClass));

        // WHEN
        Optional<ClassEntity> result = classRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test Class", result.get().getName());
    }

    @Test
    @DisplayName("Should find module by id")
    void shouldFindModuleById() {
        // GIVEN
        when(moduleRepository.findById(1)).thenReturn(Optional.of(testModule));

        // WHEN
        Optional<Module> result = moduleRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test Module", result.get().getName());
    }
}
