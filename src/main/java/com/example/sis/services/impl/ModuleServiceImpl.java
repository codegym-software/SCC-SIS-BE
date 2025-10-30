package com.example.sis.services.impl;

import com.example.sis.dtos.module.CreateModuleRequest;
import com.example.sis.dtos.module.ModuleResponse;
import com.example.sis.dtos.module.ModuleResourceDto;
import com.example.sis.dtos.module.UpdateModuleRequest;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.ConflictException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Module;
import com.example.sis.models.Program;
import com.example.sis.models.User;
import com.example.sis.repositories.ModuleRepository;
import com.example.sis.repositories.ProgramRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.ModuleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    public ModuleServiceImpl(ModuleRepository moduleRepository,
                             ProgramRepository programRepository,
                             UserRepository userRepository) {
        this.moduleRepository = moduleRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ModuleResponse createModule(CreateModuleRequest request, Integer createdBy) {
        // Validate program tồn tại
        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + request.getProgramId()));

        // Kiểm tra mã module trùng lặp trong program
        if (moduleRepository.existsByProgramIdAndCode(request.getProgramId(), request.getCode())) {
            throw new ConflictException("Mã module '" + request.getCode() + "' đã tồn tại trong program này");
        }

        // Kiểm tra sequence order trùng lặp
        if (moduleRepository.existsByProgramIdAndSequenceOrder(request.getProgramId(), request.getSequenceOrder())) {
            throw new ConflictException("Thứ tự " + request.getSequenceOrder() + " đã được sử dụng trong program này");
        }

        // AUTO-CALCULATE SEMESTER từ sequenceOrder
        // Quy tắc: 1-6 -> sem 1, 7-13 -> sem 2, 14-20 -> sem 3, 21+ -> sem 4
        Integer calculatedSemester = calculateSemesterFromSequenceOrder(request.getSequenceOrder());

        // Tạo module entity
        Module module = new Module();
        module.setProgram(program);
        module.setCode(request.getCode());
        module.setName(request.getName());
        module.setDescription(request.getDescription());
        module.setSequenceOrder(request.getSequenceOrder());
        module.setSemester(calculatedSemester);  // Sử dụng semester tự động tính, KHÔNG dùng request.getSemester()
        module.setCredits(request.getCredits());
        module.setDurationHours(request.getDurationHours());
        module.setLevel(request.getLevel());
        module.setIsMandatory(request.getIsMandatory() != null ? request.getIsMandatory() : true);
        module.setHasSyllabus(request.getHasSyllabus() != null ? request.getHasSyllabus() : false);
        module.setNotes(request.getNotes());
        module.setIsActive(true);

        // Set creator
        if (createdBy != null) {
            User creator = userRepository.findById(createdBy).orElse(null);
            module.setCreatedBy(creator);
            module.setUpdatedBy(creator);
        }

        // Lưu module
        Module savedModule = moduleRepository.save(module);

        // Trả về response
        return toResponse(savedModule);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleResponse getModuleById(Integer moduleId) {
        Module module = moduleRepository.findActiveById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));
        return toResponse(module);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesByProgramId(Integer programId) {
        // Validate program tồn tại
        programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + programId));

        List<Module> modules = moduleRepository.findByProgramIdAndActiveOrderBySequence(programId);
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ModuleResponse updateModule(Integer moduleId, UpdateModuleRequest request, Integer updatedBy) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));

        // Kiểm tra module đã bị xóa chưa
        if (module.getDeletedAt() != null) {
            throw new ConflictException("Module đã bị xóa, không thể cập nhật");
        }

        // Cập nhật code (nếu có)
        if (request.getCode() != null && !request.getCode().equals(module.getCode())) {
            if (moduleRepository.existsByProgramIdAndCodeExcludingId(
                    module.getProgramId(), request.getCode(), moduleId)) {
                throw new ConflictException("Mã module '" + request.getCode() + "' đã tồn tại trong program này");
            }
            module.setCode(request.getCode());
        }

        // KHÔNG cho phép cập nhật sequenceOrder và semester qua API này
        // Sử dụng endpoint PATCH /api/modules/reorder để thay đổi thứ tự

        // Cập nhật các field khác
        if (request.getName() != null) {
            module.setName(request.getName());
        }
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription());
        }
        if (request.getCredits() != null) {
            module.setCredits(request.getCredits());
        }
        if (request.getDurationHours() != null) {
            module.setDurationHours(request.getDurationHours());
        }
        if (request.getLevel() != null) {
            module.setLevel(request.getLevel());
        }
        if (request.getIsMandatory() != null) {
            module.setIsMandatory(request.getIsMandatory());
        }
        if (request.getHasSyllabus() != null) {
            module.setHasSyllabus(request.getHasSyllabus());
        }
        if (request.getNotes() != null) {
            module.setNotes(request.getNotes());
        }
        if (request.getIsActive() != null) {
            module.setIsActive(request.getIsActive());
        }

        // Set updater
        if (updatedBy != null) {
            User updater = userRepository.findById(updatedBy).orElse(null);
            module.setUpdatedBy(updater);
        }

        Module updatedModule = moduleRepository.save(module);
        return toResponse(updatedModule);
    }

    @Override
    @Transactional
    public void deleteModule(Integer moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));

        // Soft delete
        module.setDeletedAt(LocalDateTime.now());
        module.setIsActive(false);
        moduleRepository.save(module);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> searchModules(Integer programId, String query) {
        // Validate program tồn tại
        programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + programId));

        List<Module> modules = moduleRepository.searchByProgramIdAndQuery(programId, query);
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesByLevel(Integer programId, String level) {
        // Validate program tồn tại
        programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + programId));

        List<Module> modules = moduleRepository.findByProgramIdAndLevel(programId, level);
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getMandatoryModules(Integer programId) {
        // Validate program tồn tại
        programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + programId));

        List<Module> modules = moduleRepository.findMandatoryByProgramId(programId);
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesBySemester(Integer programId, Integer semester) {
        // Validate program tồn tại
        programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + programId));

        List<Module> modules = moduleRepository.findBySemesterAndProgramId(semester, programId);
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ModuleResponse> reorderModule(Integer moduleId, Integer newSequenceOrder, Integer updatedBy, boolean isAdminOrSA) {
        // Tìm module cần di chuyển
        Module moduleToMove = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));

        // Kiểm tra module đã bị xóa chưa
        if (moduleToMove.getDeletedAt() != null) {
            throw new ConflictException("Module đã bị xóa, không thể sắp xếp lại");
        }

        // 🔒 KIỂM TRA: Nếu KHÔNG phải ADMIN/SA, thì Module BẮT BUỘC không được phép sắp xếp
        if (!isAdminOrSA && moduleToMove.getIsMandatory()) {
            throw new ConflictException("Module BẮT BUỘC không thể sắp xếp lại. Chỉ module TỰ CHỌN mới được phép thay đổi vị trí.");
        }

        Integer programId = moduleToMove.getProgramId();
        Integer currentSequenceOrder = moduleToMove.getSequenceOrder();
        Integer currentSemester = moduleToMove.getSemester();

        // Nếu vị trí mới giống vị trí cũ, không làm gì
        if (currentSequenceOrder.equals(newSequenceOrder)) {
            return getModulesByProgramId(programId);
        }

        // Lấy tất cả modules TRONG CÙNG SEMESTER (active) để kiểm tra
        List<Module> modulesInSameSemester = moduleRepository.findBySemesterAndProgramId(
            currentSemester, 
            programId
        );

        // Tính min và max sequenceOrder trong semester hiện tại
        int minSequenceInSemester = modulesInSameSemester.stream()
                .mapToInt(Module::getSequenceOrder)
                .min()
                .orElse(currentSequenceOrder);
        
        int maxSequenceInSemester = modulesInSameSemester.stream()
                .mapToInt(Module::getSequenceOrder)
                .max()
                .orElse(currentSequenceOrder);

        // KIỂM TRA: Vị trí mới PHẢI nằm trong phạm vi semester hiện tại
        if (newSequenceOrder < minSequenceInSemester || newSequenceOrder > maxSequenceInSemester) {
            throw new ConflictException(
                String.format(
                    "Không thể di chuyển module sang semester khác! " +
                    "Module ID %d thuộc semester %d (vị trí %d-%d). " +
                    "Vị trí mới phải trong khoảng %d đến %d.",
                    moduleId, currentSemester, 
                    minSequenceInSemester, maxSequenceInSemester,
                    minSequenceInSemester, maxSequenceInSemester
                )
            );
        }

        // 🔒 KIỂM TRA: Nếu KHÔNG phải ADMIN/SA, không cho phép di chuyển qua module BẮT BUỘC
        if (!isAdminOrSA) {
            int start = Math.min(currentSequenceOrder, newSequenceOrder);
            int end = Math.max(currentSequenceOrder, newSequenceOrder);
            
            for (Module module : modulesInSameSemester) {
                // Bỏ qua module đang di chuyển và module ở vị trí hiện tại/đích
                if (module.getModuleId().equals(moduleId)) {
                    continue;
                }
                
                int moduleSeqOrder = module.getSequenceOrder();
                // Kiểm tra xem có module BẮT BUỘC nào trong khoảng di chuyển không
                if (module.getIsMandatory() && moduleSeqOrder >= start && moduleSeqOrder <= end) {
                    throw new ConflictException(
                        String.format(
                            "Không thể di chuyển qua module BẮT BUỘC '%s' (vị trí %d)! " +
                            "Các module bắt buộc có vị trí cố định và không thể bị thay đổi.",
                            module.getName(), moduleSeqOrder
                        )
                    );
                }
            }
        }

        // Lấy user để set updatedBy
        User updater = null;
        if (updatedBy != null) {
            updater = userRepository.findById(updatedBy).orElse(null);
        }

        // Logic sắp xếp lại TRONG CÙNG SEMESTER:
        // - Nếu di chuyển xuống (currentOrder < newOrder): Các module từ (currentOrder+1) đến newOrder dịch lên 1 vị trí
        // - Nếu di chuyển lên (currentOrder > newOrder): Các module từ newOrder đến (currentOrder-1) dịch xuống 1 vị trí
        // - QUAN TRỌNG: semester KHÔNG thay đổi!

        // BƯỚc 1: Set module đích về giá trị TẠM để tránh duplicate key constraint
        // Giá trị tạm = 999999 (giá trị lớn, chắc chắn không trùng với sequence thật)
        // Lưu ý: sequence_order là INT UNSIGNED nên KHÔNG thể dùng số âm
        int tempSequenceOrder = 999999;
        moduleToMove.setSequenceOrder(tempSequenceOrder);
        moduleRepository.save(moduleToMove);

        // BƯỚC 2: Dịch chuyển các module khác (CHỈ TRONG CÙNG SEMESTER!)
        if (currentSequenceOrder < newSequenceOrder) {
            // Di chuyển xuống: Module từ (current+1) đến new dịch lên
            // LỌC CHỈ LẤY MODULES TRONG CÙNG SEMESTER (loại bỏ module đang di chuyển)
            List<Module> modulesToShift = modulesInSameSemester.stream()
                .filter(m -> !m.getModuleId().equals(moduleId)) // Loại bỏ module đang di chuyển
                .filter(m -> m.getSequenceOrder() > currentSequenceOrder && m.getSequenceOrder() <= newSequenceOrder)
                .sorted((m1, m2) -> m1.getSequenceOrder().compareTo(m2.getSequenceOrder()))
                .collect(Collectors.toList());
            
            for (Module module : modulesToShift) {
                module.setSequenceOrder(module.getSequenceOrder() - 1);
                // KHÔNG thay đổi semester!
                if (updater != null) {
                    module.setUpdatedBy(updater);
                }
                // LƯU TỪNG MODULE để tránh duplicate constraint khi saveAll
                moduleRepository.save(module);
            }

        } else {
            // Di chuyển lên: Module từ new đến (current-1) dịch xuống
            // LỌC CHỈ LẤY MODULES TRONG CÙNG SEMESTER (loại bỏ module đang di chuyển)
            List<Module> modulesToShift = modulesInSameSemester.stream()
                .filter(m -> !m.getModuleId().equals(moduleId)) // Loại bỏ module đang di chuyển
                .filter(m -> m.getSequenceOrder() >= newSequenceOrder && m.getSequenceOrder() < currentSequenceOrder)
                .sorted((m1, m2) -> m2.getSequenceOrder().compareTo(m1.getSequenceOrder()))
                .collect(Collectors.toList());
            
            for (Module module : modulesToShift) {
                module.setSequenceOrder(module.getSequenceOrder() + 1);
                // KHÔNG thay đổi semester!
                if (updater != null) {
                    module.setUpdatedBy(updater);
                }
                // LƯU TỪNG MODULE để tránh duplicate constraint khi saveAll
                moduleRepository.save(module);
            }
        }

        // BƯỚC 3: Cập nhật module được di chuyển về vị trí mới chính thức
        moduleToMove.setSequenceOrder(newSequenceOrder);
        // KHÔNG thay đổi semester! Giữ nguyên semester cũ
        if (updater != null) {
            moduleToMove.setUpdatedBy(updater);
        }
        moduleRepository.save(moduleToMove);

        // Trả về danh sách modules sau khi sắp xếp lại
        return getModulesByProgramId(programId);
    }

    /**
     * Sắp xếp lại module theo sequenceOrder
     * 
     * @param programId ID của program
     * @param currentSequenceOrder Vị trí hiện tại của module
     * @param newSequenceOrder Vị trí mới
     * @param updatedBy User ID người cập nhật
     * @param isAdminOrSA true nếu người dùng là ADMIN/SA
     * @return Danh sách modules sau khi sắp xếp lại
     */
    @Override
    public List<ModuleResponse> reorderModuleBySequenceOrder(Integer programId, Integer currentSequenceOrder, Integer newSequenceOrder, Integer updatedBy, boolean isAdminOrSA) {
        // Kiểm tra programId
        if (programId == null || programId <= 0) {
            throw new BadRequestException("Program ID không hợp lệ");
        }

        // Tìm module theo programId và currentSequenceOrder
        Module moduleToMove = moduleRepository.findByProgramIdAndSequenceOrder(programId, currentSequenceOrder)
                .orElseThrow(() -> new NotFoundException(
                    "Không tìm thấy module ở vị trí " + currentSequenceOrder + " trong program ID: " + programId
                ));

        // Kiểm tra module đã bị xóa chưa
        if (moduleToMove.getDeletedAt() != null) {
            throw new ConflictException("Module đã bị xóa, không thể sắp xếp lại");
        }

        // Gọi lại method reorderModule cũ với moduleId và role
        return reorderModule(moduleToMove.getModuleId(), newSequenceOrder, updatedBy, isAdminOrSA);
    }

    /**
     * Tính semester TỰ ĐỘNG từ sequenceOrder khi tạo module
     * 
     * Quy tắc gán semester:
     * - Semester 1: sequenceOrder 1-6
     * - Semester 2: sequenceOrder 7-13
     * - Semester 3: sequenceOrder 14-20
     * - Semester 4: sequenceOrder 21+
     * 
     * ⚠️ LƯU Ý: Sau khi tạo, semester CỐ ĐỊNH và KHÔNG thay đổi khi reorder
     */
    /**
     * Tính semester TỰ ĐỘNG từ sequenceOrder
     * 
     * QUY TẮC: Cứ 6 modules liên tiếp = 1 semester
     * - Modules 1-6   → Semester 1
     * - Modules 7-12  → Semester 2
     * - Modules 13-18 → Semester 3
     * - Modules 19-24 → Semester 4
     * - ...
     * 
     * CÔNG THỨC: semester = ceil(sequenceOrder / 6)
     *           = (sequenceOrder - 1) / 6 + 1
     */
    private Integer calculateSemesterFromSequenceOrder(Integer sequenceOrder) {
        if (sequenceOrder == null || sequenceOrder <= 0) {
            return 1; // Mặc định semester 1
        }
        // Công thức: (sequenceOrder - 1) / 6 + 1
        // VD: sequenceOrder = 1  → (1-1)/6 + 1 = 0 + 1 = 1
        //     sequenceOrder = 6  → (6-1)/6 + 1 = 0 + 1 = 1
        //     sequenceOrder = 7  → (7-1)/6 + 1 = 1 + 1 = 2
        //     sequenceOrder = 12 → (12-1)/6 + 1 = 1 + 1 = 2
        //     sequenceOrder = 13 → (13-1)/6 + 1 = 2 + 1 = 3
        return (sequenceOrder - 1) / 6 + 1;
    }

    /**
     * DEPRECATED: Không sử dụng nữa!
     * 
     * Semester được gán CỐ ĐỊNH theo module ID khi tạo module.
     * Khi reorder, semester KHÔNG thay đổi, chỉ thay đổi sequenceOrder.
     * 
     * Quy tắc gán semester:
     * - Semester 1: Module ID 1-6
     * - Semester 2: Module ID 7-13  
     * - Semester 3: Module ID 14-20
     * 
     * Ví dụ:
     * - Nếu xóa module ID 6, semester 1 chỉ còn 5 modules (ID 1-5)
     * - Module ID 7 VẪN thuộc semester 2, KHÔNG tự động chuyển sang semester 1
     */
    @SuppressWarnings("unused")
    private Integer calculateSemesterByModuleId(Integer moduleId) {
        if (moduleId == null || moduleId <= 0) {
            return 1;
        }
        // Semester 1: ID 1-6, Semester 2: ID 7-13, Semester 3: ID 14-20
        // Tùy chỉnh công thức theo yêu cầu
        if (moduleId <= 6) return 1;
        if (moduleId <= 13) return 2;
        if (moduleId <= 20) return 3;
        return 4; // Mặc định semester 4 cho các ID lớn hơn
    }

    // ===== Helper Methods =====

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ModuleResponse toResponse(Module module) {
        ModuleResponse response = new ModuleResponse();
        response.setModuleId(module.getModuleId());
        response.setProgramId(module.getProgramId());
        
        // Program info
        if (module.getProgram() != null) {
            response.setProgramCode(module.getProgram().getCode());
            response.setProgramName(module.getProgram().getName());
        }

        response.setCode(module.getCode());
        response.setName(module.getName());
        response.setDescription(module.getDescription());
        response.setSequenceOrder(module.getSequenceOrder());
        response.setSemester(module.getSemester());
        response.setCredits(module.getCredits());
        response.setDurationHours(module.getDurationHours());
        response.setLevel(module.getLevel());
        response.setIsMandatory(module.getIsMandatory());
        
        // Parse JSON array từ syllabusUrl thành List<ModuleResourceDto>
        List<ModuleResourceDto> resources = parseResources(module.getSyllabusUrl());
        response.setResources(resources);
        response.setSyllabusUrl(module.getSyllabusUrl()); // Giữ để backward compatibility
        
        response.setHasSyllabus(module.getHasSyllabus());
        response.setNotes(module.getNotes());
        response.setIsActive(module.getIsActive());
        response.setDeletedAt(module.getDeletedAt());
        response.setCreatedAt(module.getCreatedAt());
        response.setUpdatedAt(module.getUpdatedAt());

        if (module.getCreatedBy() != null) {
            response.setCreatedBy(module.getCreatedBy().getUserId());
        }
        if (module.getUpdatedBy() != null) {
            response.setUpdatedBy(module.getUpdatedBy().getUserId());
        }

        return response;
    }

    /**
     * Parse JSON string thành List<ModuleResourceDto>
     */
    private List<ModuleResourceDto> parseResources(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<ModuleResourceDto>>() {});
        } catch (Exception e) {
            // Nếu parse lỗi, trả về empty list
            return new ArrayList<>();
        }
    }

    /**
     * Convert List<ModuleResourceDto> thành JSON string
     */
    private String resourcesToJson(List<ModuleResourceDto> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(resources);
        } catch (Exception e) {
            throw new BadRequestException("Không thể chuyển đổi resources thành JSON");
        }
    }

    /**
     * Tự động sắp xếp lại sequenceOrder cho tất cả modules trong program
     * Sắp xếp theo semester tăng dần, sau đó theo moduleId
     * 
     * @param programId ID của program
     * @return Danh sách modules sau khi sắp xếp lại
     */
    @Override
    @Transactional
    public List<ModuleResponse> resequenceModules(Integer programId) {
        // Kiểm tra program tồn tại
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program không tồn tại với ID: " + programId));

        // Lấy tất cả modules active trong program
        List<Module> modules = moduleRepository.findByProgramIdOrderBySemesterAscModuleIdAsc(programId);

        if (modules.isEmpty()) {
            return List.of();
        }

        // Sắp xếp lại sequenceOrder từ 1, 2, 3, ... theo thứ tự semester -> moduleId
        int newSequenceOrder = 1;
        for (Module module : modules) {
            module.setSequenceOrder(newSequenceOrder++);
            moduleRepository.save(module);
        }

        // Trả về danh sách modules sau khi sắp xếp lại
        return getModulesByProgramId(programId);
    }

    /**
     * Gắn tài liệu (resourceUrl) vào module
     * Thêm vào JSON array thay vì thay thế
     */
    @Override
    @Transactional
    public ModuleResponse attachResource(Integer moduleId, String resourceUrl, Integer updatedBy) {
        // Tìm module
        Module module = moduleRepository.findActiveById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));

        // Validate resourceUrl
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            throw new BadRequestException("Resource URL không được để trống");
        }

        // Parse JSON array hiện tại
        List<ModuleResourceDto> resources = parseResources(module.getSyllabusUrl());

        // Tạo resource mới
        ModuleResourceDto newResource = new ModuleResourceDto();
        newResource.setUrl(resourceUrl);
        newResource.setUploadedAt(LocalDateTime.now());
        newResource.setUploadedBy(updatedBy);

        // Auto-detect resource type
        if (resourceUrl.contains("youtube.com") || resourceUrl.contains("youtu.be")) {
            newResource.setFileType("YOUTUBE");
            newResource.setFileName("YouTube Video");
        } else if (resourceUrl.contains("drive.google.com")) {
            newResource.setFileType("GOOGLE_DRIVE");
            newResource.setFileName("Google Drive File");
        } else if (resourceUrl.endsWith(".pdf")) {
            newResource.setFileType("PDF");
            newResource.setFileName("PDF Document");
        } else if (resourceUrl.endsWith(".docx") || resourceUrl.endsWith(".doc")) {
            newResource.setFileType("DOCX");
            newResource.setFileName("Word Document");
        } else {
            newResource.setFileType("EXTERNAL_LINK");
            newResource.setFileName("External Link");
        }

        // Thêm resource mới vào đầu danh sách
        resources.add(0, newResource);

        // Convert về JSON
        String jsonString = resourcesToJson(resources);

        // Update module
        module.setSyllabusUrl(jsonString);
        module.setHasSyllabus(true);
        module.setUpdatedAt(LocalDateTime.now());

        // Set updater
        if (updatedBy != null) {
            User updater = userRepository.findById(updatedBy).orElse(null);
            module.setUpdatedBy(updater);
        }

        // Lưu module
        Module savedModule = moduleRepository.save(module);

        return toResponse(savedModule);
    }

    /**
     * Xóa 1 tài liệu khỏi module (theo URL)
     * Nếu xóa hết thì set syllabusUrl = null
     */
    @Override
    @Transactional
    public ModuleResponse removeResource(Integer moduleId, Integer updatedBy) {
        // Tìm module
        Module module = moduleRepository.findActiveById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));

        // Set resourceUrl = null (xóa tất cả)
        module.setSyllabusUrl(null);
        module.setHasSyllabus(false);
        module.setUpdatedAt(LocalDateTime.now());

        // Set updater
        if (updatedBy != null) {
            User updater = userRepository.findById(updatedBy).orElse(null);
            module.setUpdatedBy(updater);
        }

        // Lưu module
        Module savedModule = moduleRepository.save(module);

        return toResponse(savedModule);
    }

    /**
     * Xóa 1 tài liệu cụ thể theo URL
     */
    @Transactional
    public ModuleResponse removeResourceByUrl(Integer moduleId, String resourceUrl, Integer updatedBy) {
        // Tìm module
        Module module = moduleRepository.findActiveById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module không tồn tại với ID: " + moduleId));

        // Parse JSON array
        List<ModuleResourceDto> resources = parseResources(module.getSyllabusUrl());

        // Xóa resource có URL trùng
        resources.removeIf(r -> r.getUrl().equals(resourceUrl));

        // Convert về JSON
        String jsonString = resources.isEmpty() ? null : resourcesToJson(resources);

        // Update module
        module.setSyllabusUrl(jsonString);
        module.setHasSyllabus(!resources.isEmpty());
        module.setUpdatedAt(LocalDateTime.now());

        // Set updater
        if (updatedBy != null) {
            User updater = userRepository.findById(updatedBy).orElse(null);
            module.setUpdatedBy(updater);
        }

        // Lưu module
        Module savedModule = moduleRepository.save(module);

        return toResponse(savedModule);
    }
}




