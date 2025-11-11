package com.example.sis.services.impl;

import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.enums.OverallStatus;
import com.example.sis.models.Enrollment;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.StatusManagementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StatusManagementServiceImpl implements StatusManagementService {
    
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public StatusManagementServiceImpl(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional
    public void changeEnrollmentStatus(Integer enrollmentId, EnrollmentStatus newStatus, Integer updatedBy, String note) {
        // Tìm enrollment
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment không tồn tại với ID: " + enrollmentId));
        
        // Kiểm tra: Nếu học viên đã DROPPED thì không cho phép sửa enrollmentStatus
        Student student = enrollment.getStudent();
        if (student.getOverallStatus() == OverallStatus.DROPPED) {
            throw new RuntimeException("Không thể thay đổi trạng thái enrollment vì học viên đã nghỉ học (DROPPED)");
        }
        
        // Lưu trạng thái cũ để so sánh
        EnrollmentStatus oldStatus = enrollment.getStatus();
        
        // Cập nhật trạng thái enrollment
        enrollment.setStatus(newStatus);
        enrollment.setUpdatedAt(LocalDateTime.now());
        
        // Cập nhật left_at nếu cần
        if (newStatus == EnrollmentStatus.DROPPED || newStatus == EnrollmentStatus.GRADUATED) {
            enrollment.setLeftAt(LocalDate.now());
        } else if (newStatus == EnrollmentStatus.ACTIVE) {
            enrollment.setLeftAt(null); // Reset left_at khi chuyển về ACTIVE
        }
        
        // Cập nhật ghi chú nếu có
        if (note != null && !note.trim().isEmpty()) {
            enrollment.setNote(note);
        }
        
        // Lưu enrollment
        enrollmentRepository.save(enrollment);
        
        // Đồng bộ trạng thái Student
        syncStudentStatusFromEnrollments(enrollment.getStudent().getStudentId());
        
        // Log thay đổi
        System.out.println(String.format("Enrollment %d: %s -> %s (Student: %d)", 
                enrollmentId, oldStatus, newStatus, enrollment.getStudent().getStudentId()));
    }
    
    @Override
    @Transactional
    public void changeStudentStatus(Integer studentId, OverallStatus newStatus, Integer updatedBy, String note) {
        // Tìm student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student không tồn tại với ID: " + studentId));
        
        // Lưu trạng thái cũ
        OverallStatus oldStatus = student.getOverallStatus();
        
        // Cập nhật trạng thái student
        student.setOverallStatus(newStatus);
        student.setUpdatedAt(LocalDateTime.now());
        
        // Cập nhật người cập nhật
        if (updatedBy != null) {
            User updater = userRepository.findById(updatedBy).orElse(null);
            student.setUpdatedBy(updater);
        }
        
        // Cập nhật ghi chú nếu có
        if (note != null && !note.trim().isEmpty()) {
            student.setNote(note);
        }
        
        // Lưu student
        studentRepository.save(student);
        
        // Log thay đổi
        System.out.println(String.format("Student %d: %s -> %s", studentId, oldStatus, newStatus));
    }
    
    @Override
    @Transactional
    public void autoGraduateClassStudents(Integer classId, Integer updatedBy) {
        // Tìm tất cả enrollment ACTIVE trong lớp
        List<Enrollment> activeEnrollments = enrollmentRepository.findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(
                classId, EnrollmentStatus.ACTIVE);
        
        if (activeEnrollments.isEmpty()) {
            System.out.println("Không có học viên nào trong lớp " + classId + " để tốt nghiệp");
            return;
        }
        
        // Chuyển tất cả thành GRADUATED
        for (Enrollment enrollment : activeEnrollments) {
            changeEnrollmentStatus(enrollment.getEnrollmentId(), EnrollmentStatus.GRADUATED, updatedBy, 
                    "Tự động tốt nghiệp khi lớp hoàn thành");
        }
        
        System.out.println(String.format("Đã tốt nghiệp %d học viên trong lớp %d", 
                activeEnrollments.size(), classId));
    }
    
    @Override
    @Transactional
    public void syncStudentStatusFromEnrollments(Integer studentId) {
        // Tính toán trạng thái mới
        OverallStatus calculatedStatus = calculateStudentStatusFromEnrollments(studentId);
        
        // Cập nhật trạng thái student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student không tồn tại với ID: " + studentId));
        
        OverallStatus currentStatus = student.getOverallStatus();
        
        // Chỉ cập nhật nếu khác nhau
        if (!currentStatus.equals(calculatedStatus)) {
            student.setOverallStatus(calculatedStatus);
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);
            
            System.out.println(String.format("Đồng bộ Student %d: %s -> %s", 
                    studentId, currentStatus, calculatedStatus));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public OverallStatus calculateStudentStatusFromEnrollments(Integer studentId) {
        // Đếm các enrollment theo trạng thái
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_StudentIdAndRevokedAtIsNull(studentId);
        
        long activeCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .count();
        
        long graduatedCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.GRADUATED)
                .count();
        
        long suspendedCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.SUSPENDED)
                .count();
        
        long droppedCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.DROPPED)
                .count();
        
        // Logic xác định trạng thái Student
        if (activeCount > 0) {
            return OverallStatus.ACTIVE;
        } else if (graduatedCount > 0 && suspendedCount == 0 && droppedCount == 0) {
            return OverallStatus.GRADUATED;
        } else if (suspendedCount > 0 || droppedCount > 0) {
            // Enrollment SUSPENDED hoặc DROPPED -> Student PENDING (Đang chờ)
            // Student DROPPED chỉ set thủ công qua nút "Thay đổi trạng thái"
            return OverallStatus.PENDING;
        } else {
            return OverallStatus.PENDING; // Mặc định
        }
    }
}
