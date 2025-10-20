package com.example.sis.services.impl;

import com.example.sis.dtos.student.CreateStudentRequest;
import com.example.sis.dtos.student.StudentResponse;
import com.example.sis.dtos.student.UpdateStudentRequest;
import com.example.sis.enums.GenderType;
import com.example.sis.enums.OverallStatus;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository studentRepo;
    private final UserRepository userRepo;

    public StudentServiceImpl(StudentRepository studentRepo, UserRepository userRepo) {
        this.studentRepo = studentRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, Integer createdByUserId) {

        // 1. Validate email không trùng
        if (studentRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống");
        }

        // 2. Tạo Student entity
        Student student = new Student();
        student.setFullName(request.getFullName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDob(request.getDob());

        // Parse gender
        if (request.getGender() != null && !request.getGender().isBlank()) {
            try {
                student.setGender(GenderType.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Giới tính không hợp lệ. Chỉ chấp nhận: MALE, FEMALE, OTHER");
            }
        }

        student.setNationalIdNo(request.getNationalIdNo());
        student.setAddressLine(request.getAddressLine());
        student.setProvince(request.getProvince());
        student.setDistrict(request.getDistrict());
        student.setWard(request.getWard());
        student.setNote(request.getNote());
        student.setOverallStatus(OverallStatus.ACTIVE);

        // Set audit fields
        if (createdByUserId != null) {
            User createdByUser = userRepo.findById(createdByUserId).orElse(null);
            if (createdByUser != null) {
                student.setCreatedBy(createdByUser);
                student.setUpdatedBy(createdByUser);
            }
        }

        // 3. Lưu vào database
        student = studentRepo.save(student);
        log.info("✅ Đã tạo hồ sơ học viên - Student ID: {} - {}", student.getStudentId(), student.getFullName());

        // 4. Trả về response
        return toResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        log.info("📋 Lấy danh sách tất cả học viên (chưa xóa mềm)");
        return studentRepo.findAllActiveStudents().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Integer studentId) {
        log.info("🔍 Lấy thông tin học viên ID: {}", studentId);
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với ID: " + studentId));
        return toResponse(student);
    }

    @Override
    @Transactional
    public StudentResponse updateStudent(Integer studentId, UpdateStudentRequest request, Integer updatedByUserId) {
        log.info("✏️ Cập nhật thông tin học viên ID: {}", studentId);

        // 1. Tìm học viên cần update
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với ID: " + studentId));

        // 2. Validate email nếu thay đổi
        if (!student.getEmail().equals(request.getEmail())) {
            if (studentRepo.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã tồn tại trong hệ thống");
            }
            student.setEmail(request.getEmail());
        }

        // 3. Cập nhật 5 trường: Họ tên, SĐT, Ngày sinh, Địa chỉ
        student.setFullName(request.getFullName());
        student.setPhone(request.getPhone());
        student.setDob(request.getDob());
        student.setAddressLine(request.getAddressLine());

        // 4. Set audit field
        if (updatedByUserId != null) {
            User updatedByUser = userRepo.findById(updatedByUserId).orElse(null);
            if (updatedByUser != null) {
                student.setUpdatedBy(updatedByUser);
            }
        }

        // 5. Lưu vào database
        student = studentRepo.save(student);
        log.info("✅ Đã cập nhật hồ sơ học viên - Student ID: {} - {}", student.getStudentId(), student.getFullName());

        // 6. Trả về response
        return toResponse(student);
    }

    @Override
    @Transactional
    public void softDeleteStudent(Integer studentId) {
        log.info("🗑️ Xóa mềm học viên ID: {}", studentId);

        // 1. Tìm học viên
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với ID: " + studentId));

        // 2. Kiểm tra đã bị xóa mềm chưa
        if (student.getDeletedAt() != null) {
            throw new IllegalArgumentException("Học viên đã bị xóa trước đó");
        }

        // 3. Set deletedAt = hiện tại VÀ đổi trạng thái sang INACTIVE
        student.setDeletedAt(java.time.LocalDateTime.now());
        student.setOverallStatus(OverallStatus.INACTIVE);

        // 4. Lưu vào database
        studentRepo.save(student);
        log.info("✅ Đã xóa mềm học viên (status -> INACTIVE) - Student ID: {} - {}", student.getStudentId(), student.getFullName());
    }

    @Override
    @Transactional
    public StudentResponse updateStudentStatus(Integer studentId, String status, Integer updatedByUserId) {
        log.info("🔄 Cập nhật trạng thái học viên ID: {} -> {}", studentId, status);

        // 1. Tìm học viên
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với ID: " + studentId));

        // 2. Validate và parse status
        OverallStatus newStatus;
        try {
            newStatus = OverallStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận: ACTIVE, INACTIVE, GRADUATED, SUSPENDED");
        }

        // 3. Cập nhật trạng thái
        student.setOverallStatus(newStatus);

        // 4. Set audit field
        if (updatedByUserId != null) {
            User updatedByUser = userRepo.findById(updatedByUserId).orElse(null);
            if (updatedByUser != null) {
                student.setUpdatedBy(updatedByUser);
            }
        }

        // 5. Lưu vào database
        student = studentRepo.save(student);
        log.info("✅ Đã cập nhật trạng thái học viên - Student ID: {} - {}", student.getStudentId(), student.getFullName());

        // 6. Trả về response
        return toResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String keyword) {
        log.info("🔍 Tìm kiếm học viên với từ khóa: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents();
        }

        return studentRepo.searchByNameOrEmail(keyword.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Student entity to StudentResponse DTO
     */
    private StudentResponse toResponse(Student student) {
        StudentResponse response = new StudentResponse();
        response.setStudentId(student.getStudentId());
        response.setFullName(student.getFullName());
        response.setEmail(student.getEmail());
        response.setPhone(student.getPhone());
        response.setDob(student.getDob());
        response.setGender(student.getGender() != null ? student.getGender().name() : null);
        response.setNationalIdNo(student.getNationalIdNo());
        response.setAddressLine(student.getAddressLine());
        response.setProvince(student.getProvince());
        response.setDistrict(student.getDistrict());
        response.setWard(student.getWard());
        response.setNote(student.getNote());
        response.setOverallStatus(student.getOverallStatus().name());
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());
        return response;
    }
}
