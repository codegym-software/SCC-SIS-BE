package com.example.sis.services.impl;

import com.example.sis.dtos.enrollment.EnrollmentRequest;
import com.example.sis.dtos.enrollment.EnrollmentResponse;
import com.example.sis.dtos.enrollment.UpdateEnrollmentRequest;
import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.Enrollment;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.projections.EnrollmentListView;
import com.example.sis.services.EnrollmentService;
import com.example.sis.services.NotificationService;
import com.example.sis.services.StatusManagementService;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final EnrollmentRepository enrollmentRepo;
    private final ClassRepository classRepo;
    private final StudentRepository studentRepo;
    private final EntityManager em;
    private final StatusManagementService statusManagementService;
    private final NotificationService notificationService;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepo,
                                 ClassRepository classRepo,
                                 StudentRepository studentRepo,
                                 EntityManager em,
                                 StatusManagementService statusManagementService,
                                 NotificationService notificationService) {
        this.enrollmentRepo = enrollmentRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.em = em;
        this.statusManagementService = statusManagementService;
        this.notificationService = notificationService;
    }

    // ========= LIST =========
    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> list(Integer classId,
                                         EnrollmentStatus status,
                                         Integer page,
                                         Integer size,
                                         String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<EnrollmentListView> p = (status == null)
                ? enrollmentRepo.pageByClass(classId, pageable)
                : enrollmentRepo.pageByClassAndStatus(classId, status, pageable);
        return p.map(this::toResp);
    }

    // ========= ENROLL (IDEMPOTENT) =========
    @Override
    public EnrollmentResponse enroll(Integer classId, EnrollmentRequest req, Integer currentUserId) {
        ClassEntity clazz = classRepo.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId));

        Boolean enrollable = classRepo.isEnrollable(classId);
        if (Boolean.FALSE.equals(enrollable)) {
            throw new BadRequestException("Class is not enrollable in status " + clazz.getStatus());
        }

        Student student = studentRepo.findById(req.getStudentId())
                .orElseThrow(() -> new NotFoundException("Student not found: " + req.getStudentId()));

        LocalDate enrolledAt = (req.getEnrolledAt() != null) ? req.getEnrolledAt() : LocalDate.now();

        // Chặn trùng ACTIVE
        LocalDate today = LocalDate.now();
        var actives = enrollmentRepo.findActiveByClassAndStudent(classId, student.getStudentId(), today);
        if (!actives.isEmpty()) {
            Enrollment active = actives.get(0);
            if (enrolledAt.equals(active.getEnrolledAt())) {
                return toResp(active); // idempotent
            }
            throw new BadRequestException("Student already has an active enrollment in this class");
        }

        // Idempotent theo (class, student, enrolledAt)
        var existed = enrollmentRepo.findByClassEntity_ClassIdAndStudent_StudentIdAndEnrolledAt(
                classId, student.getStudentId(), enrolledAt);
        if (existed.isPresent()) return toResp(existed.get());

        // ===== KIỂM TRA TRÙNG LỊCH HỌC (study_days + study_time) =====
        checkScheduleConflict(student.getStudentId(), clazz, today);

        Enrollment e = new Enrollment();
        e.setClassEntity(clazz);
        e.setStudent(student);
        e.setStatus(EnrollmentStatus.ACTIVE);
        e.setEnrolledAt(enrolledAt);
        e.setNote(req.getNote());
        e.setCreatedAt(java.time.LocalDateTime.now());
        e.setUpdatedAt(java.time.LocalDateTime.now());

        if (currentUserId != null) {
            User assignedBy = em.getReference(User.class, currentUserId);
            e.setAssignedBy(assignedBy);
        }

        enrollmentRepo.save(e);
        
        // Gửi thông báo cho học viên
        if (student.getUser() != null) {
            notificationService.createAndSend(
                student.getUser().getUserId(),
                "ENROLLED_NEW_CLASS",
                "Chào mừng bạn!",
                String.format("Bạn đã được thêm vào lớp %s - Chương trình: %s",
                    clazz.getName(), clazz.getProgram().getName()),
                "class",
                clazz.getClassId().longValue(),
                "medium"
            );
        }
        
        // Gửi thông báo cho giảng viên
        if (clazz.getClassTeachers() != null && !clazz.getClassTeachers().isEmpty()) {
            clazz.getClassTeachers().stream()
                .filter(ct -> ct.getTeacher() != null)
                .forEach(ct -> {
                    notificationService.createAndSend(
                        ct.getTeacher().getUserId(),
                        "STUDENT_ENROLLED",
                        "Học viên mới",
                        String.format("%s đã được thêm vào lớp %s",
                            student.getFullName(), clazz.getName()),
                        "class",
                        clazz.getClassId().longValue(),
                        "low"
                    );
                });
        }
        
        return toResp(e);
    }

    // ========= UPDATE (STATE RULES) =========
    @Override
    public EnrollmentResponse update(Integer classId,
                                     Integer enrollmentId,
                                     UpdateEnrollmentRequest req,
                                     Integer currentUserId) {
        Enrollment e = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: " + enrollmentId));

        if (!e.getClassEntity().getClassId().equals(classId)) {
            throw new BadRequestException("Enrollment does not belong to class " + classId);
        }

        // Kiểm tra: Nếu học viên đã DROPPED thì không cho phép sửa enrollmentStatus
        Student student = e.getStudent();
        if (student.getOverallStatus() == com.example.sis.enums.OverallStatus.DROPPED) {
            throw new BadRequestException("Không thể thay đổi trạng thái enrollment vì học viên đã nghỉ học (DROPPED)");
        }

        EnrollmentStatus targetStatus = (req.getStatus() != null) ? req.getStatus() : e.getStatus();

        // (1) Không cho set leftAt khi status=ACTIVE
        if (req.getLeftAt() != null && targetStatus == EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Cannot set leftAt when status is ACTIVE");
        }

        // (2) Đổi status
        if (req.getStatus() != null) {
            if (req.getStatus() == EnrollmentStatus.ACTIVE) {
                // Về ACTIVE → clear leftAt (KHÔNG clear revoke info vì có thể là re-activate)
                e.setLeftAt(null);
            } else if ((req.getStatus() == EnrollmentStatus.DROPPED || req.getStatus() == EnrollmentStatus.SUSPENDED)
                    && req.getLeftAt() == null) {
                // Chỉ set leftAt, KHÔNG set revokedAt/revokedBy
                // revokedAt chỉ được set khi thực sự XÓA enrollment (remove method)
                e.setLeftAt(LocalDate.now());
            }
            e.setStatus(req.getStatus());
        }

        // (3) Set leftAt nếu gửi (>= enrolledAt)
        if (req.getLeftAt() != null) {
            if (e.getEnrolledAt() != null && req.getLeftAt().isBefore(e.getEnrolledAt())) {
                throw new BadRequestException("leftAt must be >= enrolledAt");
            }
            e.setLeftAt(req.getLeftAt());
            // KHÔNG set revokedAt/revokedBy ở đây
            // Chỉ set khi thực sự remove (soft delete)
        }

        // (4) Note (optional)
        if (req.getNote() != null) e.setNote(req.getNote());

        e.setUpdatedAt(java.time.LocalDateTime.now());
        enrollmentRepo.save(e);
        
        // (5) Đồng bộ trạng thái Student từ enrollments
        // Gọi StatusManagementService để tự động cập nhật student status
        statusManagementService.syncStudentStatusFromEnrollments(e.getStudent().getStudentId());
        
        return toResp(e);
    }

    // ========= REMOVE (SOFT DELETE) =========
    @Override
    public void remove(Integer classId, Integer enrollmentId, Integer currentUserId, String reason) {
        Enrollment e = enrollmentRepo.findByEnrollmentIdAndClassEntity_ClassId(enrollmentId, classId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found for classId=" + classId + ", id=" + enrollmentId));

        User actor = (currentUserId != null) ? em.getReference(User.class, currentUserId) : null;
        e.markRevoked(actor, reason);                // DROPPED + leftAt today + revokedBy/At + append note
        e.setUpdatedAt(java.time.LocalDateTime.now());
        enrollmentRepo.save(e);
        
        // Gửi thông báo cho học viên
        Student student = e.getStudent();
        ClassEntity clazz = e.getClassEntity();
        if (student.getUser() != null) {
            notificationService.createAndSend(
                student.getUser().getUserId(),
                "REMOVED_FROM_CLASS",
                "Thay đổi lớp học",
                String.format("Bạn đã bị xóa khỏi lớp %s. Vui lòng liên hệ trung tâm để biết thêm chi tiết.",
                    clazz.getName()),
                "class",
                clazz.getClassId().longValue(),
                "high"
            );
        }
        
        // Gửi thông báo cho giảng viên
        if (clazz.getClassTeachers() != null && !clazz.getClassTeachers().isEmpty()) {
            clazz.getClassTeachers().stream()
                .filter(ct -> ct.getTeacher() != null)
                .forEach(ct -> {
                    notificationService.createAndSend(
                        ct.getTeacher().getUserId(),
                        "STUDENT_REMOVED",
                        "Học viên rời lớp",
                        String.format("%s đã bị xóa khỏi lớp %s",
                            student.getFullName(), clazz.getName()),
                        "class",
                        clazz.getClassId().longValue(),
                        "medium"
                    );
                });
        }
    }

    // ========= Helpers =========
    private Pageable buildPageable(Integer page, Integer size, String sort) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 50 : Math.min(size, MAX_PAGE_SIZE);

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(p, s, Sort.by(Sort.Order.desc("enrolledAt"),
                    Sort.Order.desc("enrollmentId")));
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean asc = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1].trim());

        switch (field) {
            case "enrolledAt":
            case "leftAt":
            case "status":
            case "student.fullName":
            case "enrollmentId":
                break;
            default:
                field = "enrolledAt";
        }
        Sort.Order primary = asc ? Sort.Order.asc(field) : Sort.Order.desc(field);
        return PageRequest.of(p, s, Sort.by(primary, Sort.Order.desc("enrollmentId")));
    }

    private EnrollmentResponse toResp(EnrollmentListView v) {
        EnrollmentResponse r = new EnrollmentResponse();
        r.setEnrollmentId(v.getEnrollmentId());
        r.setClassId(v.getClassId());
        r.setStudentId(v.getStudentId());
        r.setStudentName(v.getStudentName());
        r.setStudentEmail(v.getStudentEmail());
        // Lấy student để có overallStatus
        Student student = studentRepo.findById(v.getStudentId()).orElse(null);
        if (student != null) {
            r.setStudentOverallStatus(student.getOverallStatus().name());
        }
        r.setStatus(v.getStatus().name());
        r.setEnrolledAt(v.getEnrolledAt());
        r.setLeftAt(v.getLeftAt());
        r.setNote(v.getNote());
        return r;
    }

    private EnrollmentResponse toResp(Enrollment e) {
        EnrollmentResponse r = new EnrollmentResponse();
        r.setEnrollmentId(e.getEnrollmentId());
        r.setClassId(e.getClassEntity().getClassId());
        r.setStudentId(e.getStudent().getStudentId());
        r.setStudentName(e.getStudent().getFullName());
        r.setStudentEmail(e.getStudent().getEmail());
        r.setStudentOverallStatus(e.getStudent().getOverallStatus().name());
        r.setStatus(e.getStatus().name());
        r.setEnrolledAt(e.getEnrolledAt());
        r.setLeftAt(e.getLeftAt());
        r.setNote(e.getNote());
        return r;
    }

    /**
     * Kiểm tra xung đột lịch học:
     * - Kiểm tra xem học sinh có đang học lớp nào ACTIVE khác không
     * - Nếu có trùng study_days (chỉ cần trùng 1 ngày trong 2 ngày) 
     *   -> Kiểm tra tiếp study_time
     *   -> Nếu trùng cả study_time -> throw exception
     * - Nếu không trùng ngày nào -> OK
     */
    private void checkScheduleConflict(Integer studentId, ClassEntity newClass, LocalDate today) {
        // Lấy tất cả các enrollment ACTIVE của học sinh này
        var activeEnrollments = enrollmentRepo.findAll().stream()
                .filter(e -> e.getStudent().getStudentId().equals(studentId))
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .filter(e -> e.getEffectiveEndDate() == null || e.getEffectiveEndDate().isAfter(today) || e.getEffectiveEndDate().isEqual(today))
                .toList();

        if (activeEnrollments.isEmpty()) {
            return; // Không có lớp nào đang học -> OK
        }

        // Kiểm tra xung đột với từng lớp đang học
        for (Enrollment existingEnrollment : activeEnrollments) {
            ClassEntity existingClass = existingEnrollment.getClassEntity();
            
            // Bỏ qua nếu một trong hai lớp không có thông tin lịch học
            if (newClass.getStudyDays() == null || newClass.getStudyTime() == null ||
                existingClass.getStudyDays() == null || existingClass.getStudyTime() == null) {
                continue;
            }

            // Kiểm tra xem có trùng ngày học không
            boolean hasDayConflict = false;
            for (var newDay : newClass.getStudyDays()) {
                if (existingClass.getStudyDays().contains(newDay)) {
                    hasDayConflict = true;
                    break;
                }
            }

            // Nếu không trùng ngày nào -> OK, kiểm tra lớp tiếp theo
            if (!hasDayConflict) {
                continue;
            }

            // Nếu trùng ngày, kiểm tra ca học
            if (newClass.getStudyTime() == existingClass.getStudyTime()) {
                // Trùng cả ca học -> Throw exception
                throw new BadRequestException(
                    String.format("Xung đột lịch học: Học sinh đã đăng ký lớp '%s' vào cùng ngày và ca học (%s)",
                        existingClass.getName(),
                        existingClass.getStudyTime().name())
                );
            }
            // Nếu khác ca học -> OK, cho phép đăng ký
        }
    }
}
