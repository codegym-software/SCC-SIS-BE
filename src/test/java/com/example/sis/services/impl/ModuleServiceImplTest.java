package com.example.sis.services.impl;

import com.example.sis.dtos.module.CreateModuleRequest;
import com.example.sis.dtos.module.ModuleResponse;
import com.example.sis.dtos.module.UpdateModuleRequest;
import com.example.sis.models.Module;
import com.example.sis.models.Program;
import com.example.sis.repositories.ModuleRepository;
import com.example.sis.repositories.ProgramRepository;
import com.example.sis.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleService Unit Tests")
class ModuleServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModuleServiceImpl moduleService;

    private Module testModule;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        testProgram = new Program();
        testProgram.setProgramId(1);
        testProgram.setName("Test Program");

        testModule = new Module();
        testModule.setModuleId(1);
        testModule.setName("Test Module");
        testModule.setCode("TM01");
        testModule.setProgram(testProgram);
        testModule.setSequenceOrder(1);
        testModule.setSemester(1);
    }

    @Test
    @DisplayName("Should get modules by program id")
    void shouldGetModulesByProgramId() {
        // GIVEN
        when(programRepository.findById(1)).thenReturn(Optional.of(testProgram));
        when(moduleRepository.findByProgramIdAndActiveOrderBySequence(1)).thenReturn(Arrays.asList(testModule));

        // WHEN
        List<ModuleResponse> result = moduleService.getModulesByProgramId(1);

        // THEN
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(moduleRepository, times(1)).findByProgramIdAndActiveOrderBySequence(1);
    }

    @Test
    @DisplayName("Should get module by id")
    void shouldGetModuleById() {
        // GIVEN
        when(moduleRepository.findActiveById(1)).thenReturn(Optional.of(testModule));

        // WHEN
        ModuleResponse result = moduleService.getModuleById(1);

        // THEN
        assertNotNull(result);
        assertEquals("Test Module", result.getName());
        verify(moduleRepository, times(1)).findActiveById(1);
    }

    @Test
    @DisplayName("Should create module")
    void shouldCreateModule() {
        // GIVEN
        CreateModuleRequest request = new CreateModuleRequest();
        request.setName("New Module");
        request.setCode("NM01");
        request.setProgramId(1);
        request.setSequenceOrder(1);

        when(programRepository.findById(1)).thenReturn(Optional.of(testProgram));
        when(moduleRepository.existsByProgramIdAndCode(1, "NM01")).thenReturn(false);
        when(moduleRepository.existsByProgramIdAndSequenceOrder(1, 1)).thenReturn(false);
        when(moduleRepository.save(any(Module.class))).thenReturn(testModule);

        // WHEN
        ModuleResponse result = moduleService.createModule(request, 1);

        // THEN
        assertNotNull(result);
        verify(moduleRepository, times(1)).save(any(Module.class));
    }

    @Test
    @DisplayName("Should update module")
    void shouldUpdateModule() {
        // GIVEN
        UpdateModuleRequest request = new UpdateModuleRequest();
        request.setName("Updated Module");

        when(moduleRepository.findById(1)).thenReturn(Optional.of(testModule));
        when(moduleRepository.save(any(Module.class))).thenReturn(testModule);

        // WHEN
        ModuleResponse result = moduleService.updateModule(1, request, 1);

        // THEN
        assertNotNull(result);
        verify(moduleRepository, times(1)).save(any(Module.class));
    }

    @Test
    @DisplayName("Should delete module")
    void shouldDeleteModule() {
        // GIVEN
        when(moduleRepository.findById(1)).thenReturn(Optional.of(testModule));
        when(moduleRepository.save(any(Module.class))).thenReturn(testModule);

        // WHEN
        moduleService.deleteModule(1);

        // THEN
        verify(moduleRepository, times(1)).save(any(Module.class));
    }

    @Test
    @DisplayName("Should throw exception when module not found")
    void shouldThrowExceptionWhenModuleNotFound() {
        // GIVEN
        when(moduleRepository.findActiveById(999)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(Exception.class, () -> moduleService.getModuleById(999));
    }
}
