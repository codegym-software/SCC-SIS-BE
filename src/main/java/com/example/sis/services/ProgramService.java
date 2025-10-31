package com.example.sis.services;

import com.example.sis.dtos.program.CreateProgramRequest;
import com.example.sis.dtos.program.ProgramLiteResponse;
import com.example.sis.dtos.program.ProgramResponse;
import com.example.sis.dtos.program.UpdateProgramRequest;
import com.example.sis.models.Program;
import com.example.sis.models.User;
import com.example.sis.repositories.ProgramRepository;
import com.example.sis.repositories.ModuleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
     * Tạo mới Program
     */
    public ProgramResponse createProgram(CreateProgramRequest request, User createdBy) {
        // Kiểm tra code đã tồn tại chưa
        Program existingProgram = programRepository.findByCodeAndActive(request.getCode());
        if (existingProgram != null) {
            throw new RuntimeException("Mã chương trình '" + request.getCode() + "' đã tồn tại");
        }

        // Tạo entity
        Program program = new Program();
        program.setCode(request.getCode());
        program.setName(request.getName());
        program.setDescription(request.getDescription());
        program.setDurationHours(request.getDurationHours());
        program.setDeliveryMode(request.getDeliveryMode());
        program.setCategoryCode(request.getCategoryCode());
        program.setLanguageCode(request.getLanguageCode());
        program.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        program.setCreatedAt(LocalDateTime.now());
        program.setUpdatedAt(LocalDateTime.now());
        program.setCreatedBy(createdBy);
        program.setUpdatedBy(createdBy);

        // Lưu vào database
        Program savedProgram = programRepository.save(program);

        return convertToProgramResponse(savedProgram);
    }

    /**
     * Cập nhật Program
     */
    public ProgramResponse updateProgram(Integer programId, UpdateProgramRequest request, User updatedBy) {
        // Tìm program
        Program program = programRepository.findById(programId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình với ID: " + programId));

        // Cập nhật các trường
        if (request.getName() != null) {
            program.setName(request.getName());
        }
        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getDurationHours() != null) {
            program.setDurationHours(request.getDurationHours());
        }
        if (request.getDeliveryMode() != null) {
            program.setDeliveryMode(request.getDeliveryMode());
        }
        if (request.getCategoryCode() != null) {
            program.setCategoryCode(request.getCategoryCode());
        }
        if (request.getLanguageCode() != null) {
            program.setLanguageCode(request.getLanguageCode());
        }
        if (request.getIsActive() != null) {
            program.setIsActive(request.getIsActive());
        }

        program.setUpdatedAt(LocalDateTime.now());
        program.setUpdatedBy(updatedBy);

        // Lưu vào database
        Program updatedProgram = programRepository.save(program);

        return convertToProgramResponse(updatedProgram);
    }

    /**
     * Xóa Program (soft delete)
     */
    public void deleteProgram(Integer programId, User deletedBy) {
        Program program = programRepository.findById(programId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình với ID: " + programId));

        // Kiểm tra xem có modules nào đang sử dụng không
        long moduleCount = moduleRepository.countByProgramId(programId);
        if (moduleCount > 0) {
            throw new RuntimeException("Không thể xóa chương trình vì còn " + moduleCount + " module(s) đang sử dụng");
        }

        // Soft delete
        // Để thỏa ràng buộc DB chk_program_deleted_implies_inactive: khi deleted_at != null thì is_active phải = false
        program.setIsActive(false);
        program.setDeletedAt(LocalDateTime.now());
        program.setUpdatedAt(LocalDateTime.now());
        program.setUpdatedBy(deletedBy);
        programRepository.save(program);
    }

    /**
     * Convert Program entity sang ProgramResponse (chi tiết, với module count)
     */
    private ProgramResponse convertToProgramResponse(Program program) {
        long moduleCount = moduleRepository.countByProgramId(program.getProgramId());

        ProgramResponse response = new ProgramResponse();
        response.setProgramId(program.getProgramId());
        response.setCode(program.getCode());
        response.setName(program.getName());
        response.setDescription(program.getDescription());
        response.setDurationHours(program.getDurationHours());
        response.setDeliveryMode(program.getDeliveryMode());
        response.setCategoryCode(program.getCategoryCode());
        response.setLanguageCode(program.getLanguageCode());
        response.setIsActive(program.getIsActive());
        response.setCreatedAt(program.getCreatedAt());
        response.setUpdatedAt(program.getUpdatedAt());
        response.setModuleCount((int) moduleCount);

        return response;
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