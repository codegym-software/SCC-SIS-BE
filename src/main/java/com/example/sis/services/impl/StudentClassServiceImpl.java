package com.example.sis.services.impl;

import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.Enrollment;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.services.StudentClassService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentClassServiceImpl implements StudentClassService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;

    public StudentClassServiceImpl(
            EnrollmentRepository enrollmentRepository,
            ClassRepository classRepository,
            StudentRepository studentRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public List<ClassResponse> getClassesByStudentId(Integer studentId) {
        // Kiểm tra student tồn tại
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found with ID: " + studentId);
        }

        // Lấy tất cả classes từ repository
        List<ClassEntity> allClasses = classRepository.findAll();

        // Filter classes mà student này đã enroll
        return allClasses.stream()
                .filter(classEntity -> {
                    // Kiểm tra xem student có enrollment trong class này không
                    List<Enrollment> enrollments = enrollmentRepository
                            .findActiveByClassAndStudent(
                                    classEntity.getClassId(),
                                    studentId,
                                    LocalDate.now()
                            );
                    return !enrollments.isEmpty();
                })
                .map(this::toClassResponse)
                .collect(Collectors.toList());
    }

    private ClassResponse toClassResponse(ClassEntity entity) {
        ClassResponse response = new ClassResponse();
        response.setClassId(entity.getClassId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        
        // Center info
        if (entity.getCenter() != null) {
            response.setCenterId(entity.getCenter().getCenterId());
            response.setCenterName(entity.getCenter().getName());
        }
        
        // Program info
        if (entity.getProgram() != null) {
            response.setProgramId(entity.getProgram().getProgramId());
            response.setProgramName(entity.getProgram().getName());
        }
        
        response.setStatus(entity.getStatus());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setRoom(entity.getRoom());
        response.setCapacity(entity.getCapacity());
        response.setStudyDays(entity.getStudyDays());
        response.setStudyTime(entity.getStudyTime());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        return response;
    }
}

