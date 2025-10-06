package com.example.sis.services;

import com.example.sis.dtos.classteacher.AssignLecturerRequest;
import com.example.sis.dtos.classteacher.ClassLecturerResponse;
import com.example.sis.dtos.classteacher.RemoveLecturerRequest;
import com.example.sis.exceptions.ResourceNotFoundException;
import com.example.sis.exceptions.ValidationException;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.ClassTeacher;
import com.example.sis.models.User;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.ClassTeacherRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassTeacherService {

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
    public ClassLecturerResponse assignLecturer(Integer classId, AssignLecturerRequest request, Integer assignedBy) {
        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        // Validate lecturer exists and has LECTURER role
        User lecturer = userRepository.findById(request.getLecturerId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Lecturer not found with id: " + request.getLecturerId()));

        // Kiểm tra lecturer có role LECTURER không
        String lecturerKeycloakId = lecturer.getKeycloakUserId();
        if (lecturerKeycloakId == null
                || !userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(lecturerKeycloakId, "LECTURER")) {
            throw new ValidationException("User with id " + request.getLecturerId() + " does not have LECTURER role");
        }

        // Kiểm tra lecturer đã được gán cho lớp này chưa
        Optional<ClassTeacher> existingAssignment = classTeacherRepository
                .findActiveAssignment(classId, request.getLecturerId());

        if (existingAssignment.isPresent()) {
            throw new ValidationException("Lecturer is already assigned to this class");
        }

        // Kiểm tra có conflict về start_date không (tránh vi phạm unique constraint)
        LocalDate startDate = LocalDate.now();
        long conflicts = classTeacherRepository.countConflictingAssignments(
                classId, request.getLecturerId(), startDate, startDate);

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
        classTeacher.setCreatedAt(LocalDateTime.now());
        classTeacher.setUpdatedAt(LocalDateTime.now()); // Cần set updated_at vì NOT NULL

        ClassTeacher savedAssignment = classTeacherRepository.save(classTeacher);

        return mapToResponse(savedAssignment);
    }

    /**
     * Xóa lecturer khỏi lớp học (soft delete bằng cách set end_date)
     */
    public void removeLecturer(Integer classId, RemoveLecturerRequest request, Integer revokedBy) {
        // Tìm assignment hiện tại
        ClassTeacher assignment = classTeacherRepository
                .findActiveAssignment(classId, request.getLecturerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active assignment not found for lecturer " + request.getLecturerId() + " in class "
                                + classId));

        // Lấy User entity cho revokedBy
        User revokedByUser = revokedBy != null ? userRepository.findById(revokedBy).orElse(null) : null;

        // Set end date = hôm nay để "remove" assignment
        assignment.setEndDate(LocalDate.now());
        assignment.setRevokedBy(revokedByUser);
        assignment.setUpdatedAt(LocalDateTime.now());

        classTeacherRepository.save(assignment);
    }

    /**
     * Lấy danh sách lecturers đang được gán cho lớp
     */
    @Transactional(readOnly = true)
    public List<ClassLecturerResponse> getActiveLecturers(Integer classId) {
        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }

        List<ClassTeacher> assignments = classTeacherRepository.findActiveByClassId(classId);
        return assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả lecturers assignments cho lớp (bao gồm cả inactive)
     */
    @Transactional(readOnly = true)
    public List<ClassLecturerResponse> getAllLecturers(Integer classId) {
        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }

        List<ClassTeacher> assignments = classTeacherRepository.findAllByClassId(classId);
        return assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map ClassTeacher entity to DTO
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
}