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

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepo,
                                 ClassRepository classRepo,
                                 StudentRepository studentRepo,
                                 EntityManager em) {
        this.enrollmentRepo = enrollmentRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.em = em;
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

        EnrollmentStatus targetStatus = (req.getStatus() != null) ? req.getStatus() : e.getStatus();

        // (1) Không cho set leftAt khi status=ACTIVE
        if (req.getLeftAt() != null && targetStatus == EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Cannot set leftAt when status is ACTIVE");
        }

        // (2) Đổi status
        if (req.getStatus() != null) {
            if (req.getStatus() == EnrollmentStatus.ACTIVE) {
                // Về ACTIVE → clear leftAt + clear revoke info
                e.setLeftAt(null);
                e.setRevokedBy(null);
                e.setRevokedAt(null);
            } else if ((req.getStatus() == EnrollmentStatus.DROPPED || req.getStatus() == EnrollmentStatus.SUSPENDED)
                    && req.getLeftAt() == null) {
                e.setLeftAt(LocalDate.now());
                if (currentUserId != null) {
                    e.setRevokedBy(em.getReference(User.class, currentUserId));
                }
                e.setRevokedAt(java.time.LocalDateTime.now());
            }
            e.setStatus(req.getStatus());
        }

        // (3) Set leftAt nếu gửi (>= enrolledAt)
        if (req.getLeftAt() != null) {
            if (e.getEnrolledAt() != null && req.getLeftAt().isBefore(e.getEnrolledAt())) {
                throw new BadRequestException("leftAt must be >= enrolledAt");
            }
            e.setLeftAt(req.getLeftAt());
            if (e.getStatus() != EnrollmentStatus.ACTIVE) {
                if (currentUserId != null) e.setRevokedBy(em.getReference(User.class, currentUserId));
                e.setRevokedAt(java.time.LocalDateTime.now());
            }
        }

        // (4) Note (optional)
        if (req.getNote() != null) e.setNote(req.getNote());

        e.setUpdatedAt(java.time.LocalDateTime.now());
        enrollmentRepo.save(e);
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
        r.setStatus(e.getStatus().name());
        r.setEnrolledAt(e.getEnrolledAt());
        r.setLeftAt(e.getLeftAt());
        r.setNote(e.getNote());
        return r;
    }
}
