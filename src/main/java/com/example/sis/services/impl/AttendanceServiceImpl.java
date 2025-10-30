package com.example.sis.services.impl;

import com.example.sis.dtos.attendance.*;
import com.example.sis.enums.AttendanceStatus;
import com.example.sis.enums.StudyDay;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.*;
import com.example.sis.repositories.*;
import com.example.sis.services.AttendanceService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;
    private final ClassRepository classRepo;
    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final ClassTeacherRepository classTeacherRepo;
    private final EntityManager em;

    public AttendanceServiceImpl(
            AttendanceSessionRepository sessionRepo,
            AttendanceRecordRepository recordRepo,
            ClassRepository classRepo,
            StudentRepository studentRepo,
            EnrollmentRepository enrollmentRepo,
            UserRepository userRepo,
            ClassTeacherRepository classTeacherRepo,
            EntityManager em) {
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.userRepo = userRepo;
        this.classTeacherRepo = classTeacherRepo;
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherScheduleResponse> getTeacherSchedule(Integer teacherId, LocalDate from, LocalDate to) {
        // 1. Lấy tất cả các lớp mà giảng viên này được gán (active assignments)
        List<ClassTeacher> assignments = classTeacherRepo.findActiveByTeacherId(teacherId);
        
        List<TeacherScheduleResponse> result = new ArrayList<>();
        
        // 2. Với mỗi lớp, tính toán tất cả các ngày học CỤ THỂ trong khoảng thời gian
        for (ClassTeacher assignment : assignments) {
            ClassEntity clazz = assignment.getClassEntity();
            
            // Validate dữ liệu
            if (clazz.getStartDate() == null || clazz.getEndDate() == null 
                || clazz.getStudyDays() == null || clazz.getStudyDays().isEmpty()) {
                continue;
            }
            
            // Tính các ngày học cụ thể trong khoảng from-to
            List<LocalDate> studyDates = calculateStudyDates(
                clazz.getStartDate(),
                clazz.getEndDate(),
                clazz.getStudyDays(),
                from,
                to
            );
            
            // 3. Tạo response cho mỗi ngày học
            for (LocalDate studyDate : studyDates) {
                TeacherScheduleResponse scheduleItem = new TeacherScheduleResponse();
                scheduleItem.setClassId(clazz.getClassId());
                scheduleItem.setClassName(clazz.getName());
                scheduleItem.setAttendanceDate(studyDate);
                
                // Check xem đã có buổi điểm danh chưa
                boolean hasSession = sessionRepo.existsByClassEntity_ClassIdAndAttendanceDateAndDeletedFalse(
                    clazz.getClassId(), 
                    studyDate
                );
                
                scheduleItem.setSessionStatus(hasSession ? "TAKEN" : "NOT_TAKEN");
                
                result.add(scheduleItem);
            }
        }
        
        // Sắp xếp theo ngày
        result.sort((a, b) -> a.getAttendanceDate().compareTo(b.getAttendanceDate()));
        
        return result;
    }
    
    /**
     * Tính toán danh sách các ngày học CỤ THỂ trong khoảng thời gian
     * 
     * Ví dụ: Lớp học thứ 2 & thứ 6 từ 1/1/2025 đến 31/1/2025
     * => Trả về: [6/1, 10/1, 13/1, 17/1, 20/1, 24/1, 27/1, 31/1]
     */
    private List<LocalDate> calculateStudyDates(
        LocalDate classStartDate,
        LocalDate classEndDate,
        List<StudyDay> studyDays,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        List<LocalDate> result = new ArrayList<>();
        
        if (studyDays == null || studyDays.isEmpty()) {
            return result;
        }
        
        // Xác định khoảng thời gian thực tế cần tính (intersection)
        LocalDate start = classStartDate.isAfter(fromDate) ? classStartDate : fromDate;
        LocalDate end = classEndDate.isBefore(toDate) ? classEndDate : toDate;
        
        if (start.isAfter(end)) {
            return result;
        }
        
        LocalDate current = start;
        
        // Duyệt qua tất cả các ngày trong khoảng
        while (!current.isAfter(end)) {
            // Chuyển DayOfWeek sang StudyDay
            StudyDay matchingStudyDay = convertToStudyDay(current.getDayOfWeek());
            
            // Nếu ngày này nằm trong lịch học của lớp
            if (studyDays.contains(matchingStudyDay)) {
                result.add(current);
            }
            
            current = current.plusDays(1);
        }
        
        return result;
    }
    
    /**
     * Chuyển đổi java.time.DayOfWeek sang enum StudyDay
     */
    private StudyDay convertToStudyDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> StudyDay.MONDAY;
            case TUESDAY -> StudyDay.TUESDAY;
            case WEDNESDAY -> StudyDay.WEDNESDAY;
            case THURSDAY -> StudyDay.THURSDAY;
            case FRIDAY -> StudyDay.FRIDAY;
            case SATURDAY -> StudyDay.SATURDAY;
            case SUNDAY -> StudyDay.SUNDAY;
        };
    }

    @Override
    public AttendanceSessionResponse createSession(CreateAttendanceSessionRequest request, Integer currentUserId) {
        // Validate class exists
        ClassEntity classEntity = classRepo.findById(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));

        // Validate teacher exists
        User teacher = userRepo.findById(request.getTeacherId())
                .orElseThrow(() -> new NotFoundException("Teacher not found: " + request.getTeacherId()));

        // Check if attendance already taken for this date
        if (sessionRepo.existsByClassEntity_ClassIdAndAttendanceDateAndDeletedFalse(
                request.getClassId(), request.getAttendanceDate())) {
            throw new BadRequestException("Attendance already taken for this class on " + request.getAttendanceDate());
        }

        // Create session
        AttendanceSession session = new AttendanceSession();
        session.setClassEntity(classEntity);
        session.setTeacher(teacher);
        session.setAttendanceDate(request.getAttendanceDate());
        session.setNotes(request.getNotes());
        session.setTotalStudents(request.getRecords().size());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        if (currentUserId != null) {
            User createdBy = em.getReference(User.class, currentUserId);
            session.setCreatedBy(createdBy);
        }

        // Calculate present/absent counts
        int presentCount = 0;
        int absentCount = 0;

        // Create records
        List<AttendanceRecord> records = new ArrayList<>();
        for (CreateAttendanceSessionRequest.AttendanceRecordRequest recordReq : request.getRecords()) {
            AttendanceRecord record = new AttendanceRecord();
            
            Enrollment enrollment = enrollmentRepo.findById(recordReq.getEnrollmentId())
                    .orElseThrow(() -> new NotFoundException("Enrollment not found: " + recordReq.getEnrollmentId()));
            record.setEnrollment(enrollment);

            Student student = studentRepo.findById(recordReq.getStudentId())
                    .orElseThrow(() -> new NotFoundException("Student not found: " + recordReq.getStudentId()));
            record.setStudent(student);

            record.setSession(session);
            record.setStatus(AttendanceStatus.valueOf(recordReq.getStatus()));
            record.setNotes(recordReq.getNotes());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());

            if (currentUserId != null) {
                User createdBy = em.getReference(User.class, currentUserId);
                record.setCreatedBy(createdBy);
            }

            records.add(record);

            if (record.getStatus() == AttendanceStatus.PRESENT) {
                presentCount++;
            } else {
                absentCount++;
            }
        }

        session.setPresentCount(presentCount);
        session.setAbsentCount(absentCount);

        // Save session (cascade saves records)
        session.setRecords(records);
        AttendanceSession savedSession = sessionRepo.save(session);

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSessionSummaryResponse> getSessionsByClass(Integer classId) {
        // Fetch sessions without loading relationships
        List<AttendanceSession> sessions = sessionRepo.findByClassEntity_ClassIdAndDeletedFalseOrderByAttendanceDateDesc(classId);
        
        // Map to DTO immediately to avoid lazy loading issues
        List<AttendanceSessionSummaryResponse> result = new ArrayList<>();
        for (AttendanceSession session : sessions) {
            AttendanceSessionSummaryResponse response = new AttendanceSessionSummaryResponse();
            response.setSessionId(session.getSessionId());
            response.setAttendanceDate(session.getAttendanceDate());
            response.setTotalStudents(session.getTotalStudents());
            response.setPresentCount(session.getPresentCount());
            response.setAbsentCount(session.getAbsentCount());
            result.add(response);
        }
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSessionResponse getSessionDetail(Integer sessionId) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Attendance session not found: " + sessionId));

        if (session.getDeleted()) {
            throw new NotFoundException("Attendance session not found: " + sessionId);
        }

        AttendanceSessionResponse response = toSessionResponse(session);
        
        // Get records
        List<AttendanceRecordResponse> records = recordRepo.findBySessionId(sessionId);
        response.setRecords(records);

        return response;
    }

    @Override
    public AttendanceSessionResponse updateSession(Integer sessionId, UpdateAttendanceSessionRequest request, Integer currentUserId) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Attendance session not found: " + sessionId));

        if (session.getDeleted()) {
            throw new NotFoundException("Attendance session not found: " + sessionId);
        }

        // Update notes if provided
        if (request.getNotes() != null) {
            session.setNotes(request.getNotes());
        }

        // Update records
        if (request.getRecords() != null && !request.getRecords().isEmpty()) {
            int presentCount = 0;
            int absentCount = 0;

            for (UpdateAttendanceSessionRequest.UpdateAttendanceRecordRequest recordReq : request.getRecords()) {
                AttendanceRecord record = session.getRecords().stream()
                        .filter(r -> r.getRecordId().equals(recordReq.getRecordId()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Record not found: " + recordReq.getRecordId()));

                record.setStatus(AttendanceStatus.valueOf(recordReq.getStatus()));
                
                if (recordReq.getNotes() != null) {
                    record.setNotes(recordReq.getNotes());
                }

                if (currentUserId != null) {
                    User updatedBy = em.getReference(User.class, currentUserId);
                    record.setUpdatedBy(updatedBy);
                }
                record.setUpdatedAt(LocalDateTime.now());

                if (record.getStatus() == AttendanceStatus.PRESENT) {
                    presentCount++;
                } else {
                    absentCount++;
                }
            }

            session.setPresentCount(presentCount);
            session.setAbsentCount(absentCount);
        }

        if (currentUserId != null) {
            User updatedBy = em.getReference(User.class, currentUserId);
            session.setUpdatedBy(updatedBy);
        }
        session.setUpdatedAt(LocalDateTime.now());

        AttendanceSession updatedSession = sessionRepo.save(session);
        return toSessionResponse(updatedSession);
    }

    @Override
    public void deleteSession(Integer sessionId, Integer currentUserId) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Attendance session not found: " + sessionId));

        if (session.getDeleted()) {
            throw new NotFoundException("Attendance session not found: " + sessionId);
        }

        session.setDeleted(true);
        session.setDeletedAt(LocalDateTime.now());

        if (currentUserId != null) {
            User deletedBy = em.getReference(User.class, currentUserId);
            session.setDeletedBy(deletedBy);
        }

        sessionRepo.save(session);
    }

    private AttendanceSessionResponse toSessionResponse(AttendanceSession session) {
        AttendanceSessionResponse response = new AttendanceSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setClassId(session.getClassEntity().getClassId());
        response.setClassName(session.getClassEntity().getName());
        response.setTeacherId(session.getTeacher().getUserId());
        response.setTeacherName(session.getTeacher().getFullName());
        response.setAttendanceDate(session.getAttendanceDate());
        response.setNotes(session.getNotes());
        response.setTotalStudents(session.getTotalStudents());
        response.setPresentCount(session.getPresentCount());
        response.setAbsentCount(session.getAbsentCount());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    private AttendanceSessionSummaryResponse toSummaryResponse(AttendanceSession session) {
        AttendanceSessionSummaryResponse response = new AttendanceSessionSummaryResponse();
        response.setSessionId(session.getSessionId());
        response.setAttendanceDate(session.getAttendanceDate());
        response.setTotalStudents(session.getTotalStudents());
        response.setPresentCount(session.getPresentCount());
        response.setAbsentCount(session.getAbsentCount());
        return response;
    }
}

