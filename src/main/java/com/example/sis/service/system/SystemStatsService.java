package com.example.sis.service.system;

import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.LessonProgressRepository;
import com.example.sis.repositories.ModuleRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.models.ClassEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemStatsService {
    
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final ModuleRepository moduleRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRoleRepository userRoleRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Get total number of users in system
     */
    public Long getTotalUsers() {
        return userRepository.count();
    }
    
    /**
     * Get total number of students (users with STUDENT role)
     */
    public Long getTotalStudents() {
        String jpql = "SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur " +
                     "JOIN ur.role r WHERE r.code = 'STUDENT' AND ur.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get total number of admins
     */
    public Long getTotalAdmins() {
        String jpql = "SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur " +
                     "JOIN ur.role r WHERE r.code IN ('SUPER_ADMIN', 'CENTER_MANAGER') AND ur.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get total number of instructors
     */
    public Long getTotalInstructors() {
        String jpql = "SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur " +
                     "JOIN ur.role r WHERE r.code = 'LECTURER' AND ur.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get active students (enrolled in at least one class)
     */
    public Long getActiveStudents() {
        String jpql = "SELECT COUNT(DISTINCT e.student.studentId) FROM Enrollment e " +
                     "WHERE e.status = 'ACTIVE' AND e.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get class start date and calculate days until start
     * @param className Class name (e.g., "Java K-17")
     * @return Map with start_date, days_until_start, status
     */
    public Map<String, Object> getClassStartInfo(String className) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Query class by name
            String jpql = "SELECT c FROM ClassEntity c WHERE c.name = :className AND c.deletedAt IS NULL";
            ClassEntity classEntity = entityManager.createQuery(jpql, ClassEntity.class)
                    .setParameter("className", className)
                    .getSingleResult();
            
            LocalDate startDate = classEntity.getStartDate();
            // ⚠️ CRITICAL: Use Vietnam timezone (GMT+7) instead of server timezone
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            
            log.info("🕐 Vietnam time today: {}, Class start date: {}", today, startDate);
            
            long daysUntilStart = ChronoUnit.DAYS.between(today, startDate);
            
            result.put("class_name", className);
            result.put("start_date", startDate.toString());
            result.put("days_until_start", daysUntilStart);
            
            if (daysUntilStart > 0) {
                result.put("status", "upcoming");
                result.put("message", String.format("Lớp sẽ bắt đầu sau %d ngày", daysUntilStart));
            } else if (daysUntilStart == 0) {
                result.put("status", "starting_today");
                result.put("message", "Lớp bắt đầu hôm nay");
            } else {
                result.put("status", "started");
                result.put("message", String.format("Lớp đã bắt đầu được %d ngày", Math.abs(daysUntilStart)));
            }
            
            result.put("success", true);
            
        } catch (jakarta.persistence.NoResultException e) {
            log.warn("Class not found: {}", className);
            result.put("success", false);
            result.put("error", String.format("Không tìm thấy lớp '%s'", className));
        } catch (Exception e) {
            log.error("Failed to get class start info for: {}", className, e);
            result.put("success", false);
            result.put("error", "Có lỗi xảy ra khi truy vấn thông tin lớp");
        }
        
        return result;
    }
    
    /**
     * Get comprehensive system statistics
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_users", getTotalUsers());
        stats.put("total_students", getTotalStudents());
        stats.put("active_students", getActiveStudents());
        stats.put("total_admins", getTotalAdmins());
        stats.put("total_instructors", getTotalInstructors());
        stats.put("timestamp", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());
        
        return stats;
    }
    
    /**
     * Get enrollment statistics for a specific class
     */
    public Map<String, Object> getClassEnrollmentStats(String className) {
        Map<String, Object> stats = new HashMap<>();
        
        // TODO: Implement when ClassRepository is available
        // Long enrolled = enrollmentRepository.countByClassName(className);
        // Long capacity = classRepository.findByClassName(className).getCapacity();
        
        stats.put("class_name", className);
        stats.put("enrolled_students", 0L); // Replace with real data
        stats.put("capacity", 0L); // Replace with real data
        stats.put("available_slots", 0L); // Replace with real data
        
        return stats;
    }
}
