package com.example.sis.services;

import com.example.sis.dtos.classteacher.AssignLecturerRequest;
import com.example.sis.dtos.classteacher.BatchAssignLecturerRequest;
import com.example.sis.dtos.classteacher.BatchAssignLecturerResponse;
import com.example.sis.dtos.classteacher.BatchAssignLecturerItem;
import com.example.sis.dtos.classteacher.ClassLecturerResponse;
import com.example.sis.dtos.classteacher.ClassLecturerItem;
import com.example.sis.dtos.classteacher.LecturerLite;
import com.example.sis.dtos.classteacher.ListResponse;
import com.example.sis.exceptions.ResourceNotFoundException;
import com.example.sis.exceptions.ValidationException;
import com.example.sis.exceptions.LecturerAlreadyAssignedException;
import com.example.sis.exceptions.ClassMaxActiveLecturersExceededException;
import com.example.sis.exceptions.AssignmentNotFoundException;
import com.example.sis.exceptions.ConflictException;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.ClassTeacher;
import com.example.sis.models.User;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.ClassTeacherRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class ClassTeacherService {

    private static final Logger log = LoggerFactory.getLogger(ClassTeacherService.class);

    private final ClassTeacherRepository classTeacherRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public ClassTeacherService(ClassTeacherRepository classTeacherRepository,
            ClassRepository classRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository) {
        this.classTeacherRepository = classTeacherRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Gán lecturer vào lớp học
     */
    public ClassLecturerResponse assignLecturer(Integer classId, Integer lecturerId, AssignLecturerRequest request,
            Integer assignedBy) {
        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        // Validate lecturer exists and has LECTURER role
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Lecturer not found with id: " + lecturerId));

        // Kiểm tra lecturer có role LECTURER không
        String lecturerKeycloakId = lecturer.getKeycloakUserId();
        if (lecturerKeycloakId == null
                || !userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(lecturerKeycloakId, "LECTURER")) {
            throw new ValidationException("User with id " + lecturerId + " does not have LECTURER role");
        }

        // Kiểm tra lecturer đã được gán cho lớp này chưa
        Optional<ClassTeacher> existingAssignment = classTeacherRepository
                .findActiveAssignment(classId, lecturerId);

        if (existingAssignment.isPresent()) {
            throw new ValidationException("Lecturer is already assigned to this class");
        }

        // Kiểm tra có conflict về start_date không (tránh vi phạm unique constraint)
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        long conflicts = classTeacherRepository.countConflictingAssignments(
                classId, lecturerId, startDate, startDate);

        if (conflicts > 0) {
            throw new ValidationException("Lecturer has already been assigned to this class on " + startDate);
        }

        // Lấy User entity cho assignedBy
        User assignedByUser = assignedBy != null ? userRepository.findById(assignedBy).orElse(null) : null;

        // Tạo assignment mới
        ClassTeacher classTeacher = new ClassTeacher();
        classTeacher.setClassEntity(classEntity);
        classTeacher.setTeacher(lecturer);
        classTeacher.setStartDate(startDate);
        classTeacher.setEndDate(null); // Chưa có ngày kết thúc
        classTeacher.setAssignedBy(assignedByUser);
        classTeacher.setNote(request.getNote()); // Set note từ request
        classTeacher.setCreatedAt(LocalDateTime.now());
        classTeacher.setUpdatedAt(LocalDateTime.now()); // Cần set updated_at vì NOT NULL

        ClassTeacher savedAssignment = classTeacherRepository.save(classTeacher);

        return mapToResponse(savedAssignment);
    }


    /**
     * Lấy danh sách lecturers đang được gán cho lớp (active assignments)
     */
    @Transactional(readOnly = true)
    public ListResponse<ClassLecturerItem> getActiveLecturers(Integer classId, String searchQuery) {
        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }

        List<ClassTeacher> assignments = classTeacherRepository.findActiveByClassIdWithSearch(classId, searchQuery);
        long total = classTeacherRepository.countActiveByClassIdWithSearch(classId, searchQuery);

        // Debug logging
        System.out.println("DEBUG - Found " + assignments.size() + " assignments for class " + classId);
        System.out.println("DEBUG - Count query returned: " + total);

        List<ClassLecturerItem> items = assignments.stream()
                .map(this::mapToClassLecturerItem)
                .collect(Collectors.toList());

        System.out.println("DEBUG - Mapped to " + items.size() + " DTO items");

        return new ListResponse<>(total, items);
    }

    /**
     * Lấy tất cả lecturers assignments cho lớp (bao gồm cả inactive)
     */
    @Transactional(readOnly = true)
    public ListResponse<ClassLecturerItem> getAllLecturers(Integer classId, String status, String searchQuery) {
        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }

        String statusFilter = status != null ? status : "all";
        List<ClassTeacher> assignments = classTeacherRepository.findAllByClassIdWithFilterAndSearch(classId, statusFilter, searchQuery);
        long total = classTeacherRepository.countAllByClassIdWithFilterAndSearch(classId, statusFilter, searchQuery);

        List<ClassLecturerItem> items = assignments.stream()
                .map(this::mapToClassLecturerItem)
                .collect(Collectors.toList());

        return new ListResponse<>(total, items);
    }

    /**
     * Lấy danh sách giảng viên available để phân công cho lớp
     * (những giảng viên thuộc cùng center, chưa có assignment active trong lớp này)
     */
    @Transactional(readOnly = true)
    public ListResponse<LecturerLite> getAvailableLecturers(Integer classId, String searchQuery) {
        // Validate class exists và lấy thông tin center
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        Integer centerId = classEntity.getCenter().getCenterId();

        // Lấy danh sách giảng viên thuộc center và có role LECTURER
        // Loại trừ những giảng viên đã có assignment active trong lớp này
        List<User> availableLecturers = userRepository.findAvailableLecturersByCenterAndClass(
                centerId, classId, searchQuery);

        long total = availableLecturers.size();

        // Map to LecturerLite DTO
        List<LecturerLite> items = availableLecturers.stream()
                .map(user -> new LecturerLite(
                        (long) user.getUserId(),
                        user.getFullName(),
                        user.getEmail(),
                        null // avatarUrl - tạm thời null vì User model chưa có field này
                ))
                .collect(Collectors.toList());

        return new ListResponse<>(total, items);
    }

    /**
     * Gán nhiều giảng viên cho lớp (batch assignment)
     */
    public BatchAssignLecturerResponse batchAssignLecturers(Integer classId,
            BatchAssignLecturerRequest request, Integer assignedBy) {
        // Validate class exists và lấy thông tin center
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        Integer centerId = classEntity.getCenter().getCenterId();

        // Đếm số lượng giảng viên active hiện tại
        long currentActiveCount = classTeacherRepository.countActiveByClassId(classId);

        // Kiểm tra giới hạn tối đa 3 giảng viên active
        int maxLecturers = 3;
        if (currentActiveCount >= maxLecturers) {
            throw new ClassMaxActiveLecturersExceededException(
                "Lớp đã đạt tối đa " + maxLecturers + " giảng viên active");
        }

        int created = 0;
        List<Integer> skipped = new ArrayList<>();

        // Lấy User entity cho assignedBy
        User assignedByUser = assignedBy != null ? userRepository.findById(assignedBy).orElse(null) : null;

        for (var item : request.items()) {
            Integer lecturerId = item.lecturerId();

            try {
                // Validate lecturer exists và thuộc cùng center
                User lecturer = userRepository.findById(lecturerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found with id: " + lecturerId));

                // Kiểm tra lecturer có role LECTURER không
                String lecturerKeycloakId = lecturer.getKeycloakUserId();
                if (lecturerKeycloakId == null
                        || !userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(lecturerKeycloakId, "LECTURER")) {
                    throw new ValidationException("User with id " + lecturerId + " does not have LECTURER role");
                }

                // Kiểm tra lecturer đã được gán cho lớp này chưa
                Optional<ClassTeacher> existingAssignment = classTeacherRepository
                        .findActiveAssignment(classId, lecturerId);

                if (existingAssignment.isPresent()) {
                    skipped.add(lecturerId);
                    continue;
                }

                // Kiểm tra có conflict về start_date không
                LocalDate startDate = item.startDate() != null ? item.startDate() : LocalDate.now();
                long conflicts = classTeacherRepository.countConflictingAssignments(
                        classId, lecturerId, startDate, startDate);

                if (conflicts > 0) {
                    skipped.add(lecturerId);
                    continue;
                }

                // Kiểm tra sau khi thêm sẽ vượt quá giới hạn không
                if (currentActiveCount + created >= maxLecturers) {
                    throw new ClassMaxActiveLecturersExceededException(
                        "Không thể gán thêm giảng viên. Lớp đã đạt tối đa " + maxLecturers + " giảng viên active");
                }

                // Tạo assignment mới
                ClassTeacher classTeacher = new ClassTeacher();
                classTeacher.setClassEntity(classEntity);
                classTeacher.setTeacher(lecturer);
                classTeacher.setStartDate(startDate);
                classTeacher.setEndDate(null);
                classTeacher.setAssignedBy(assignedByUser);
                classTeacher.setNote(item.note());
                classTeacher.setCreatedAt(LocalDateTime.now());
                classTeacher.setUpdatedAt(LocalDateTime.now());

                classTeacherRepository.save(classTeacher);
                created++;

            } catch (Exception e) {
                // Nếu có lỗi với lecturer này, bỏ qua và tiếp tục với lecturer khác
                skipped.add(lecturerId);
            }
        }

        return new BatchAssignLecturerResponse(created, skipped);
    }

    /**
     * Revoke assignment (soft delete) - implementation theo yêu cầu
     */
    @Transactional
    public void revokeAssignment(Integer classId, Long assignmentId) {
        log.info("DELETE assignment: classId={}, assignmentId={}", classId, assignmentId);

        // Tìm assignment theo yêu cầu (chỉ tìm những assignment có effEndDate IS NULL)
        var ct = classTeacherRepository.findByIdAndClassIdAndEffEndDateIsNull(assignmentId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("ASSIGNMENT_NOT_FOUND"));

        // Kiểm tra assignment đã bị revoke chưa (effEndDate < CURRENT_DATE)
        if (ct.getEffEndDate().isBefore(LocalDate.now(ZoneId.systemDefault()))) {
            throw new ConflictException("ASSIGNMENT_ALREADY_REVOKED");
        }

        // Set các field để revoke assignment
        var today = LocalDate.now(ZoneId.systemDefault());
        ct.setEndDate(today);
        ct.setEffEndDate(today);
        ct.setRevokedBy(null); // TODO: Set current user when available
        ct.setUpdatedAt(LocalDateTime.now());

        classTeacherRepository.save(ct);
    }

    /**
     * Map ClassTeacher entity to ClassLecturerItem DTO
     */
    private ClassLecturerItem mapToClassLecturerItem(ClassTeacher classTeacher) {
        User teacher = classTeacher.getTeacher();

        // Tạo LecturerLite từ thông tin teacher
        LecturerLite lecturerLite = new LecturerLite(
                (long) teacher.getUserId(),
                teacher.getFullName(),
                teacher.getEmail(),
                null // avatarUrl - cần thêm field này vào User model nếu cần
        );

        // Xác định trạng thái active dựa trên endDate IS NULL
        boolean isActive = classTeacher.getEndDate() == null;

        return new ClassLecturerItem(
                (long) classTeacher.getClassTeacherId(),
                (long) classTeacher.getClassEntity().getClassId(),
                lecturerLite,
                classTeacher.getStartDate(),
                classTeacher.getEndDate(),
                isActive,
                classTeacher.getNote(),
                classTeacher.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant(),
                "System", // assignedBy tạm thời
                null, // revokedBy
                true, // canEdit - cần logic xác định quyền
                true  // canRemove - cần logic xác định quyền
        );
    }

    /**
     * Map ClassTeacher entity to DTO (deprecated - chỉ để tương thích)
     */
    private ClassLecturerResponse mapToResponse(ClassTeacher classTeacher) {
        User teacher = classTeacher.getTeacher();

        return new ClassLecturerResponse(
                classTeacher.getClassTeacherId(),
                classTeacher.getClassEntity().getClassId(),
                teacher.getUserId(),
                teacher.getFullName(),
                teacher.getEmail(),
                classTeacher.getStartDate().atStartOfDay(), // Convert LocalDate to LocalDateTime
                classTeacher.getEndDate() != null ? classTeacher.getEndDate().atStartOfDay() : null,
                classTeacher.getCreatedAt(),
                classTeacher.getAssignedBy() != null ? classTeacher.getAssignedBy().getUserId().toString() : null);
    }

    /**
     * Lấy danh sách lớp học mà giảng viên đang được gán
     * Chỉ trả về các lớp có assignment đang active
     */
    public List<com.example.sis.dtos.classes.ClassLiteResponse> getClassesByTeacherId(Integer teacherId) {
        // Validate teacher exists
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        // Kiểm tra teacher có role LECTURER không
        String lecturerKeycloakId = teacher.getKeycloakUserId();
        if (lecturerKeycloakId == null
                || !userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(lecturerKeycloakId, "LECTURER")) {
            throw new ValidationException("User with id " + teacherId + " does not have LECTURER role");
        }

        // Lấy danh sách lớp từ assignments đang active
        List<ClassTeacher> activeAssignments = classTeacherRepository.findActiveByTeacherId(teacherId);

        // Map sang ClassLiteResponse
        return activeAssignments.stream()
                .map(ct -> {
                    ClassEntity classEntity = ct.getClassEntity();
                    return new com.example.sis.dtos.classes.ClassLiteResponse(
                            classEntity.getClassId(),
                            classEntity.getName(),
                            classEntity.getProgram().getName(),
                            classEntity.getCenter().getName(),
                            classEntity.getStatus());
                })
                .collect(Collectors.toList());
    }
}