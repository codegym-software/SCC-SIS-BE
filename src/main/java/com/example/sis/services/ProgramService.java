package com.example.sis.services;

import com.example.sis.dtos.program.ProgramLiteResponse;
import com.example.sis.models.Program;
import com.example.sis.repositories.ProgramRepository;
import com.example.sis.repositories.ModuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ModuleRepository moduleRepository;

    public ProgramService(ProgramRepository programRepository, ModuleRepository moduleRepository) {
        this.programRepository = programRepository;
        this.moduleRepository = moduleRepository;
    }

    /**
     * Lấy danh sách tất cả Programs đang hoạt động cho dropdown
     */
    public List<ProgramLiteResponse> getAllActivePrograms() {
        List<Program> programs = programRepository.findAllActivePrograms();
        return programs.stream()
                .map(this::convertToProgramLiteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách Programs theo category
     */
    public List<ProgramLiteResponse> getProgramsByCategory(String categoryCode) {
        List<Program> programs = programRepository.findByCategoryCodeAndActive(categoryCode);
        return programs.stream()
                .map(this::convertToProgramLiteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra Program có tồn tại và đang hoạt động không
     */
    public boolean isProgramActiveAndExists(Integer programId) {
        return programRepository.existsByIdAndActive(programId);
    }

    /**
     * Lấy Program theo ID
     */
    public Program getProgramById(Integer programId) {
        return programRepository.findById(programId)
                .filter(program -> program.getIsActive() && program.getDeletedAt() == null)
                .orElse(null);
    }

    /**
     * Convert Program entity sang ProgramLiteResponse (với module count)
     */
    private ProgramLiteResponse convertToProgramLiteResponse(Program program) {
        // Đếm số lượng modules thuộc program này
        long moduleCount = moduleRepository.countByProgramId(program.getProgramId());
        
        return new ProgramLiteResponse(
                program.getProgramId(),
                program.getCode(),
                program.getName(),
                program.getDescription(),
                program.getDurationHours(),
                program.getDeliveryMode(),
                program.getCategoryCode(),
                program.getIsActive(),
                moduleCount);
    }
}