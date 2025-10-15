package com.example.sis.services;

import com.example.sis.dtos.classes.ClassLiteResponse;
import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.dtos.classes.CreateClassRequest;
import com.example.sis.dtos.classes.UpdateClassRequest;
import com.example.sis.models.Center;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.Program;
import com.example.sis.models.User;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.ProgramRepository;
import com.example.sis.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final ProgramRepository programRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;

    public ClassService(ClassRepository classRepository, ProgramRepository programRepository,
            CenterRepository centerRepository, UserRepository userRepository) {
        this.classRepository = classRepository;
        this.programRepository = programRepository;
        this.centerRepository = centerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Tạo lớp học mới
     */
    @Transactional
    public ClassResponse createClass(CreateClassRequest request, Integer centerId, Integer createdBy) {
        // Validate center exists
        Center center = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new RuntimeException("Trung tâm không tồn tại hoặc đã bị vô hiệu hóa"));

        // Validate program exists and is active
        Program program = programRepository.findById(request.getProgramId())
                .filter(p -> p.getIsActive() && p.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Chương trình học không tồn tại hoặc đã bị vô hiệu hóa"));

        // Check class name uniqueness in center
        if (classRepository.existsByCenterIdAndName(centerId, request.getName())) {
            throw new RuntimeException("Tên lớp học đã tồn tại trong trung tâm này");
        }

        // Validate dates if provided
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
            }
        }

        // Get created user
        User creator = userRepository.findById(createdBy).orElse(null);

        // Create new class
        ClassEntity classEntity = new ClassEntity();
        classEntity.setCenter(center);
        classEntity.setProgram(program);
        classEntity.setName(request.getName());
        classEntity.setDescription(request.getDescription());
        classEntity.setStartDate(request.getStartDate());
        classEntity.setEndDate(request.getEndDate());
        classEntity.setRoom(request.getRoom());
        classEntity.setCapacity(request.getCapacity());
        classEntity.setStatus(ClassEntity.ClassStatus.PLANNED);

        // Set study schedule
        classEntity.setStudyDays(request.getStudyDays());
        classEntity.setStudyTime(request.getStudyTime());

        classEntity.setCreatedAt(LocalDateTime.now());
        classEntity.setUpdatedAt(LocalDateTime.now());
        classEntity.setCreatedBy(creator);
        classEntity.setUpdatedBy(creator);

        ClassEntity savedClass = classRepository.save(classEntity);
        return convertToClassResponse(savedClass);
    }

    /**
     * Cập nhật thông tin lớp học
     */
    @Transactional
    public ClassResponse updateClass(Integer classId, UpdateClassRequest request, Integer updatedBy) {
        // Find existing class
        ClassEntity existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Lớp học không tồn tại"));

        // Check if new name is unique within the center (excluding current class)
        if (!existingClass.getName().equals(request.getName()) &&
                classRepository.existsByCenterIdAndNameExcludingId(
                        existingClass.getCenter().getCenterId(), request.getName(), classId)) {
            throw new RuntimeException("Tên lớp học đã tồn tại trong trung tâm này");
        }

        // Validate dates if provided
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
            }
        }

        // Get updater user
        User updater = userRepository.findById(updatedBy).orElse(null);

        // Update fields
        existingClass.setName(request.getName());
        existingClass.setDescription(request.getDescription());
        existingClass.setStartDate(request.getStartDate());
        existingClass.setEndDate(request.getEndDate());
        existingClass.setRoom(request.getRoom());
        existingClass.setCapacity(request.getCapacity());

        // Update study schedule
        existingClass.setStudyDays(request.getStudyDays());
        existingClass.setStudyTime(request.getStudyTime());

        existingClass.setUpdatedAt(LocalDateTime.now());
        existingClass.setUpdatedBy(updater);

        ClassEntity savedClass = classRepository.save(existingClass);
        return convertToClassResponse(savedClass);
    }

    /**
     * Lấy danh sách tất cả lớp học (cho Super Admin)
     */
    public List<ClassResponse> getAllClasses() {
        List<ClassEntity> classes = classRepository.findAllOrderByStartDateDesc();
        return classes.stream()
                .map(this::convertToClassResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách lớp học theo trung tâm (cho Academic Staff)
     */
    public List<ClassResponse> getClassesByCenter(Integer centerId) {
        List<ClassEntity> classes = classRepository.findByCenterIdOrderByStartDateDesc(centerId);
        return classes.stream()
                .map(this::convertToClassResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách lớp học theo trạng thái
     */
    public List<ClassResponse> getClassesByStatus(ClassEntity.ClassStatus status) {
        List<ClassEntity> classes = classRepository.findByStatusOrderByStartDateDesc(status);
        return classes.stream()
                .map(this::convertToClassResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách lớp học theo trung tâm và trạng thái
     */
    public List<ClassResponse> getClassesByCenterAndStatus(Integer centerId, ClassEntity.ClassStatus status) {
        List<ClassEntity> classes = classRepository.findByCenterIdAndStatusOrderByStartDateDesc(centerId, status);
        return classes.stream()
                .map(this::convertToClassResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lớp học theo ID
     */
    public ClassResponse getClassById(Integer classId) {
        ClassEntity classEntity = classRepository.findByIdAndNotDeleted(classId)
                .orElseThrow(() -> new RuntimeException("Lớp học không tồn tại"));
        return convertToClassResponse(classEntity);
    }

    /**
     * Lấy danh sách lớp học lite cho dropdown
     */
    public List<ClassLiteResponse> getClassesLite() {
        List<ClassEntity> classes = classRepository.findAllOrderByStartDateDesc();
        return classes.stream()
                .map(this::convertToClassLiteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert ClassEntity sang ClassResponse
     */
    private ClassResponse convertToClassResponse(ClassEntity classEntity) {
        ClassResponse response = new ClassResponse();
        response.setClassId(classEntity.getClassId());
        response.setCenterId(classEntity.getCenter().getCenterId());
        response.setCenterName(classEntity.getCenter().getName());
        response.setProgramId(classEntity.getProgram().getProgramId());
        response.setProgramName(classEntity.getProgram().getName());
        response.setProgramCode(classEntity.getProgram().getCode());
        response.setName(classEntity.getName());
        response.setDescription(classEntity.getDescription());
        response.setStartDate(classEntity.getStartDate());
        response.setEndDate(classEntity.getEndDate());
        response.setStatus(classEntity.getStatus());
        response.setRoom(classEntity.getRoom());
        response.setCapacity(classEntity.getCapacity());
        response.setStudyDays(classEntity.getStudyDays());
        response.setStudyTime(classEntity.getStudyTime());
        response.setCreatedAt(classEntity.getCreatedAt());
        response.setUpdatedAt(classEntity.getUpdatedAt());
        response.setCreatedBy(classEntity.getCreatedBy() != null ? classEntity.getCreatedBy().getUserId() : null);
        response.setUpdatedBy(classEntity.getUpdatedBy() != null ? classEntity.getUpdatedBy().getUserId() : null);
        return response;
    }

    /**
     * Convert ClassEntity sang ClassLiteResponse
     */
    private ClassLiteResponse convertToClassLiteResponse(ClassEntity classEntity) {
        return new ClassLiteResponse(
                classEntity.getClassId(),
                classEntity.getName(),
                classEntity.getProgram().getName(),
                classEntity.getCenter().getName(),
                classEntity.getStatus());
    }
}