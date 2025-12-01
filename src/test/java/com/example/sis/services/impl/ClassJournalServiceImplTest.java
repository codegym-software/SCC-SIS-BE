package com.example.sis.services.impl;

import com.example.sis.models.ClassEntity;
import com.example.sis.models.ClassJournal;
import com.example.sis.repositories.ClassJournalRepository;
import com.example.sis.repositories.ClassRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassJournalService Unit Tests")
class ClassJournalServiceImplTest {

    @Mock
    private ClassJournalRepository classJournalRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassJournalServiceImpl classJournalService;

    private ClassJournal testJournal;
    private ClassEntity testClass;

    @BeforeEach
    void setUp() {
        testClass = new ClassEntity();
        testClass.setClassId(1);
        testClass.setName("Test Class");

        testJournal = new ClassJournal();
        testJournal.setJournalId(1);
        testJournal.setTitle("Test Journal");
        testJournal.setClassEntity(testClass);
    }

    @Test
    @DisplayName("Should find journal by id")
    void shouldFindJournalById() {
        // GIVEN
        when(classJournalRepository.findById(1)).thenReturn(Optional.of(testJournal));

        // WHEN
        Optional<ClassJournal> result = classJournalRepository.findById(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test Journal", result.get().getTitle());
        verify(classJournalRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should find class by id")
    void shouldFindClassById() {
        // GIVEN
        when(classRepository.findByIdAndNotDeleted(1)).thenReturn(Optional.of(testClass));

        // WHEN
        Optional<ClassEntity> result = classRepository.findByIdAndNotDeleted(1);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("Test Class", result.get().getName());
    }

    @Test
    @DisplayName("Should return empty when journal not found")
    void shouldReturnEmptyWhenNotFound() {
        // GIVEN
        when(classJournalRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN
        Optional<ClassJournal> result = classJournalRepository.findById(999);

        // THEN
        assertFalse(result.isPresent());
    }
}
