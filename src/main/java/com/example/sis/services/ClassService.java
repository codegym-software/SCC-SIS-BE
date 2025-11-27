package com.example.sis.services;

import com.example.sis.dtos.classes.ClassLiteResponse;
import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.dtos.classes.CreateClassRequest;
import com.example.sis.dtos.classes.UpdateClassRequest;
import com.example.sis.enums.StudyDay;
import com.example.sis.models.Center;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.Program;
import com.example.sis.models.User;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.ProgramRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.NotificationService;
import com.example.sis.services.StatusManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final ProgramRepository programRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final StatusManagementService statusManagementService;
    private final NotificationService notificationService;

    public ClassService(ClassRepository classRepository, ProgramRepository programRepository,
            CenterRepository centerRepository, UserRepository userRepository, 
            StatusManagementService statusManagementService,
            NotificationService notificationService) {
        this.classRepository = classRepository;
        this.programRepository = programRepository;
        this.centerRepository = centerRepository;
        this.userRepository = userRepository;
        this.statusManagementService = statusManagementService;
        this.notificationService = notificationService;
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
        LocalDate today = LocalDate.now();
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
            }
            
            // Validate startDate >= today
            if (request.getStartDate().isBefore(today)) {
                throw new RuntimeException("Thời gian bắt đầu phải ở tương lai (không được trước thời gian hiện tại)");
            }
            
            // Validate endDate > today
            if (!request.getEndDate().isAfter(today)) {
                throw new RuntimeException("Thời gian kết thúc phải sau thời gian hiện tại trên máy tính");
            }

            // Validate study days based on date range
            if (request.getStudyDays() != null && !request.getStudyDays().isEmpty()) {
                validateStudyDays(request.getStartDate(), request.getEndDate(), request.getStudyDays());
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
        
        // Status sẽ được tính tự động từ ngày bắt đầu/kết thúc
        // Không cần set status ở đây nữa

        // Set study schedule
        classEntity.setStudyDays(request.getStudyDays());
        classEntity.setStudyTime(request.getStudyTime());

        classEntity.setCreatedAt(LocalDateTime.now());
        classEntity.setUpdatedAt(LocalDateTime.now());
        classEntity.setCreatedBy(creator);
        classEntity.setUpdatedBy(creator);

        // Tính status tự động từ ngày và lưu vào DB
        ClassEntity.ClassStatus calculatedStatus = calculateStatus(classEntity);
        classEntity.setStatus(calculatedStatus);

        ClassEntity savedClass = classRepository.save(classEntity);

        // Force load lazy relationships before converting to response
        savedClass.getCenter().getName(); // Trigger lazy load
        savedClass.getProgram().getName(); // Trigger lazy load
        if (savedClass.getCreatedBy() != null) {
            savedClass.getCreatedBy().getUserId(); // Trigger lazy load
        }

        // Gửi thông báo cho admin/manager về lớp mới (bao gồm người tạo)
        notificationService.notifyAdminsExcept(
                savedClass.getCenter().getCenterId(),
                createdBy,
                "CLASS_CREATED",
                "Lớp học mới được tạo",
                String.format("Lớp %s (%s) đã được tạo tại %s",
                        savedClass.getName(),
                        savedClass.getProgram().getName(),
                        savedClass.getCenter().getName()),
                "CLASS",
                savedClass.getClassId().longValue(),
                "INFO"
        );

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
        if (request.getName() != null && 
                !existingClass.getName().equals(request.getName()) &&
                classRepository.existsByCenterIdAndNameExcludingId(
                        existingClass.getCenter().getCenterId(), request.getName(), classId)) {
            throw new RuntimeException("Tên lớp học đã tồn tại trong trung tâm này");
        }

        // Validate dates if provided
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
            }

            // Validate study days based on date range
            if (request.getStudyDays() != null && !request.getStudyDays().isEmpty()) {
                validateStudyDays(request.getStartDate(), request.getEndDate(), request.getStudyDays());
            }
        }

        // Get updater user
        User updater = userRepository.findById(updatedBy).orElse(null);

        // Update fields only if provided (partial update)
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            existingClass.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingClass.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            existingClass.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            existingClass.setEndDate(request.getEndDate());
        }
        if (request.getRoom() != null) {
            existingClass.setRoom(request.getRoom());
        }
        if (request.getCapacity() != null) {
            existingClass.setCapacity(request.getCapacity());
        }

        // Update study schedule
        if (request.getStudyDays() != null) {
            existingClass.setStudyDays(request.getStudyDays());
        }
        if (request.getStudyTime() != null) {
            existingClass.setStudyTime(request.getStudyTime());
        }

        // Update status
        if (request.getStatus() != null) {
            // Có request.getStatus() từ frontend
            if (request.getStatus() == ClassEntity.ClassStatus.CANCELLED) {
                // User muốn tạm dừng class - set CANCELLED
                existingClass.setStatus(ClassEntity.ClassStatus.CANCELLED);
            } else {
                // User muốn set status khác - set trực tiếp (dùng cho khôi phục)
                existingClass.setStatus(request.getStatus());
            }
        } else {
            // Không có request.getStatus() - nếu hiện tại đang CANCELLED và có update khác, giữ nguyên CANCELLED
            // Nếu không phải CANCELLED, tính status tự động từ ngày
            if (existingClass.getStatus() != ClassEntity.ClassStatus.CANCELLED) {
                ClassEntity.ClassStatus calculatedStatus = calculateStatus(existingClass);
                existingClass.setStatus(calculatedStatus);
            }
        }

        existingClass.setUpdatedAt(LocalDateTime.now());
        existingClass.setUpdatedBy(updater);

        ClassEntity savedClass = classRepository.save(existingClass);

        // Force load lazy relationships before converting to response
        savedClass.getCenter().getName(); // Trigger lazy load
        savedClass.getProgram().getName(); // Trigger lazy load
        if (savedClass.getUpdatedBy() != null) {
            savedClass.getUpdatedBy().getUserId(); // Trigger lazy load
        }
        
        // Gửi thông báo khi có thay đổi quan trọng (lịch học, phòng, thời gian)
        boolean hasImportantChanges = request.getStartDate() != null || 
                                      request.getEndDate() != null ||
                                      request.getStudyDays() != null ||
                                      request.getStudyTime() != null ||
                                      request.getRoom() != null;
        
        if (hasImportantChanges && savedClass.getClassTeachers() != null) {
            // Thông báo cho giảng viên
            savedClass.getClassTeachers().stream()
                .filter(ct -> ct.getTeacher() != null)
                .forEach(ct -> {
                    notificationService.createAndSend(
                        ct.getTeacher().getUserId(),
                        "CLASS_UPDATED",
                        "Cập nhật lớp học",
                        String.format("Lớp %s đã có thay đổi thông tin - Vui lòng kiểm tra chi tiết",
                            savedClass.getName()),
                        "class",
                        savedClass.getClassId().longValue(),
                        "medium"
                    );
                });
        }

        return convertToClassResponse(savedClass);
    }

    /**
     * Xóa lớp học (soft delete)
     */
    @Transactional
    public void deleteClass(Integer classId, Integer deletedBy) {
        // Find existing class
        ClassEntity existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Lớp học không tồn tại"));

        // Check if already deleted
        if (existingClass.getDeletedAt() != null) {
            throw new RuntimeException("Lớp học đã bị xóa");
        }

        // Perform soft delete
        existingClass.setDeletedAt(LocalDateTime.now());
        existingClass.setUpdatedAt(LocalDateTime.now());
        
        User deleter = userRepository.findById(deletedBy).orElse(null);
        existingClass.setUpdatedBy(deleter);

        classRepository.save(existingClass);
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
     * Tính toán status tự động dựa trên ngày bắt đầu và kết thúc
     * - CANCELLED: Nếu có trong DB thì giữ nguyên (chỉ có thể set manual)
     * - PLANNED: Nếu ngày hiện tại < startDate
     * - ONGOING: Nếu ngày hiện tại >= startDate và <= endDate
     * - FINISHED: Nếu ngày hiện tại > endDate
     */
    private ClassEntity.ClassStatus calculateStatus(ClassEntity classEntity) {
        // Nếu status là CANCELLED, giữ nguyên
        if (classEntity.getStatus() == ClassEntity.ClassStatus.CANCELLED) {
            return ClassEntity.ClassStatus.CANCELLED;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = classEntity.getStartDate();
        LocalDate endDate = classEntity.getEndDate();

        // Nếu không có ngày, trả về PLANNED làm mặc định
        if (startDate == null || endDate == null) {
            return ClassEntity.ClassStatus.PLANNED;
        }

        // Tính status dựa trên ngày
        ClassEntity.ClassStatus calculatedStatus;
        if (today.isBefore(startDate)) {
            calculatedStatus = ClassEntity.ClassStatus.PLANNED;
        } else if (today.isAfter(endDate)) {
            calculatedStatus = ClassEntity.ClassStatus.FINISHED;
        } else {
            calculatedStatus = ClassEntity.ClassStatus.ONGOING;
        }
        
        // Nếu lớp vừa chuyển sang FINISHED, tự động tốt nghiệp tất cả học viên
        if (calculatedStatus == ClassEntity.ClassStatus.FINISHED && 
            classEntity.getStatus() != ClassEntity.ClassStatus.FINISHED) {
            try {
                statusManagementService.autoGraduateClassStudents(classEntity.getClassId(), null);
                System.out.println("Đã tự động tốt nghiệp học viên trong lớp " + classEntity.getClassId());
            } catch (Exception e) {
                System.err.println("Lỗi khi tự động tốt nghiệp học viên: " + e.getMessage());
            }
        }
        
        return calculatedStatus;
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
        
        // Tính status tự động từ ngày
        response.setStatus(calculateStatus(classEntity));
        
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

    /**
     * Validate study days against date range
     * Logic:
     * - Nếu khoảng thời gian >= 7 ngày (1 tuần): Chấp nhận mọi ngày học
     * - Nếu khoảng thời gian < 7 ngày: Chỉ chấp nhận các ngày nằm trong khoảng
     * startDate -> endDate
     */
    private void validateStudyDays(LocalDate startDate, LocalDate endDate, List<StudyDay> studyDays) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        // Nếu khoảng thời gian >= 7 ngày (1 tuần), chấp nhận mọi ngày
        if (daysBetween >= 7) {
            return;
        }

        // Nếu < 7 ngày, kiểm tra các ngày học có nằm trong khoảng không
        Set<DayOfWeek> validDaysOfWeek = new HashSet<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            validDaysOfWeek.add(current.getDayOfWeek());
            current = current.plusDays(1);
        }

        for (StudyDay studyDay : studyDays) {
            DayOfWeek dayOfWeek = convertStudyDayToDayOfWeek(studyDay);
            if (!validDaysOfWeek.contains(dayOfWeek)) {
                throw new RuntimeException(
                        String.format("Ngày học '%s' không hợp lệ. Lớp học < 7 ngày (từ %s đến %s), " +
                                "các ngày học phải nằm trong khoảng này.",
                                getStudyDayVietnameseName(studyDay),
                                getVietnameseDayOfWeek(startDate.getDayOfWeek()),
                                getVietnameseDayOfWeek(endDate.getDayOfWeek())));
            }
        }
    }

    /**
     * Convert StudyDay enum to Java DayOfWeek
     */
    private DayOfWeek convertStudyDayToDayOfWeek(StudyDay studyDay) {
        switch (studyDay) {
            case MONDAY:
                return DayOfWeek.MONDAY;
            case TUESDAY:
                return DayOfWeek.TUESDAY;
            case WEDNESDAY:
                return DayOfWeek.WEDNESDAY;
            case THURSDAY:
                return DayOfWeek.THURSDAY;
            case FRIDAY:
                return DayOfWeek.FRIDAY;
            case SATURDAY:
                return DayOfWeek.SATURDAY;
            case SUNDAY:
                return DayOfWeek.SUNDAY;
            default:
                throw new IllegalArgumentException("Invalid StudyDay: " + studyDay);
        }
    }

    /**
     * Get Vietnamese name for StudyDay
     */
    private String getStudyDayVietnameseName(StudyDay studyDay) {
        switch (studyDay) {
            case MONDAY:
                return "Thứ 2";
            case TUESDAY:
                return "Thứ 3";
            case WEDNESDAY:
                return "Thứ 4";
            case THURSDAY:
                return "Thứ 5";
            case FRIDAY:
                return "Thứ 6";
            case SATURDAY:
                return "Thứ 7";
            case SUNDAY:
                return "Chủ nhật";
            default:
                return studyDay.name();
        }
    }

    /**
     * Get Vietnamese name for DayOfWeek
     */
    private String getVietnameseDayOfWeek(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "Thứ 2";
            case TUESDAY:
                return "Thứ 3";
            case WEDNESDAY:
                return "Thứ 4";
            case THURSDAY:
                return "Thứ 5";
            case FRIDAY:
                return "Thứ 6";
            case SATURDAY:
                return "Thứ 7";
            case SUNDAY:
                return "Chủ nhật";
            default:
                return dayOfWeek.name();
        }
    }

    /**
     * Lấy danh sách lớp học mà giảng viên được phân công
     * @param lecturerId ID của giảng viên
     * @return Danh sách lớp học
     */
    public List<ClassResponse> getClassesByLecturer(Integer lecturerId) {
        List<ClassEntity> classes = classRepository.findClassesByLecturerId(lecturerId);
        return classes.stream()
                .map(this::convertToClassResponse)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra giảng viên có được phân công vào lớp không
     * @param lecturerId ID của giảng viên
     * @param classId ID của lớp học
     * @return true nếu giảng viên được phân công và còn hiệu lực
     */
    public boolean isLecturerAssignedToClass(Integer lecturerId, Integer classId) {
        return classRepository.isLecturerAssignedToClass(lecturerId, classId);
    }
}