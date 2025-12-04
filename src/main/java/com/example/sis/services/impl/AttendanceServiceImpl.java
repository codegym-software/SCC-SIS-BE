package com.example.sis.services.impl;

import com.example.sis.dtos.attendance.*;
import com.example.sis.enums.AttendanceStatus;
import com.example.sis.enums.StudyDay;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.*;
import com.example.sis.repositories.*;
import com.example.sis.services.AttendanceService;
import com.example.sis.services.NotificationService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final NotificationService notificationService;

    public AttendanceServiceImpl(
            AttendanceSessionRepository sessionRepo,
            AttendanceRecordRepository recordRepo,
            ClassRepository classRepo,
            StudentRepository studentRepo,
            EnrollmentRepository enrollmentRepo,
            UserRepository userRepo,
            ClassTeacherRepository classTeacherRepo,
            EntityManager em,
            NotificationService notificationService) {
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.userRepo = userRepo;
        this.classTeacherRepo = classTeacherRepo;
        this.em = em;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherScheduleResponse> getTeacherSchedule(Integer teacherId, LocalDate from, LocalDate to) {
        // 1. Lấy tất cả các lớp mà giảng viên này được gán (active assignments)
        List<ClassTeacher> assignments = classTeacherRepo.findActiveByTeacherId(teacherId);
        
        // Map để lưu tất cả events: key = "classId-date", value = TeacherScheduleResponse
        Map<String, TeacherScheduleResponse> eventMap = new HashMap<>();
        
        // 2. Với mỗi lớp, xử lý sessions và lịch lý thuyết
        for (ClassTeacher assignment : assignments) {
            ClassEntity clazz = assignment.getClassEntity();
            Integer classId = clazz.getClassId();
            
            // 2.1. TRƯỚC TIÊN: Lấy TẤT CẢ các AttendanceSession đã tạo trong khoảng thời gian
            List<AttendanceSession> existingSessions = sessionRepo.findByClassEntity_ClassIdAndAttendanceDateBetweenAndDeletedFalse(
                classId,
                from,
                to
            );
            
            // Thêm tất cả sessions đã tạo vào eventMap (ưu tiên cao nhất)
            for (AttendanceSession session : existingSessions) {
                String key = classId + "-" + session.getAttendanceDate().toString();
                TeacherScheduleResponse scheduleItem = new TeacherScheduleResponse();
                scheduleItem.setClassId(classId);
                scheduleItem.setClassName(clazz.getName());
                scheduleItem.setAttendanceDate(session.getAttendanceDate());
                scheduleItem.setSessionStatus("TAKEN"); // Đã điểm danh
                
                // Sử dụng studyTime từ session (lưu tại thời điểm điểm danh)
                // thay vì từ class hiện tại để giữ nguyên khung giờ ban đầu
                scheduleItem.setStudyTime(session.getStudyTime());
                
                eventMap.put(key, scheduleItem);
            }
            
            // 2.2. SAU ĐÓ: Tính lịch lý thuyết từ studyDays (chỉ cho ngày CHƯA có session)
            if (clazz.getStartDate() != null && clazz.getEndDate() != null 
                && clazz.getStudyDays() != null && !clazz.getStudyDays().isEmpty()) {
                
                // Số buổi học quy định trong 1 tuần (theo studyDays của class)
                int sessionsPerWeek = clazz.getStudyDays().size();
                
                // Tính các ngày học cụ thể trong khoảng from-to
                List<LocalDate> studyDates = calculateStudyDates(
                    clazz.getStartDate(),
                    clazz.getEndDate(),
                    clazz.getStudyDays(),
                    from,
                    to
                );
                
                // Tạo response cho mỗi ngày học (chỉ nếu chưa có trong eventMap)
                for (LocalDate studyDate : studyDates) {
                    String key = classId + "-" + studyDate.toString();
                    
                    // Chỉ thêm nếu chưa có session cho ngày này
                    if (!eventMap.containsKey(key)) {
                        // Kiểm tra xem tuần này đã có đủ số buổi học chưa
                        LocalDate weekStart = studyDate.with(java.time.DayOfWeek.MONDAY);
                        LocalDate weekEnd = weekStart.plusDays(6);
                        
                        long takenSessionsInWeek = existingSessions.stream()
                            .filter(s -> !s.getAttendanceDate().isBefore(weekStart) 
                                      && !s.getAttendanceDate().isAfter(weekEnd))
                            .count();
                        
                        // Chỉ thêm buổi mới nếu tuần chưa đủ số buổi học
                        if (takenSessionsInWeek < sessionsPerWeek) {
                            TeacherScheduleResponse scheduleItem = new TeacherScheduleResponse();
                            scheduleItem.setClassId(classId);
                            scheduleItem.setClassName(clazz.getName());
                            scheduleItem.setAttendanceDate(studyDate);
                            scheduleItem.setSessionStatus("NOT_TAKEN"); // Chưa điểm danh
                            // Sử dụng studyTime hiện tại của class cho lịch chưa điểm danh
                            if (clazz.getStudyTime() != null) {
                                scheduleItem.setStudyTime(clazz.getStudyTime().name());
                            }
                            eventMap.put(key, scheduleItem);
                        }
                    }
                }
            }
        }
        
        // 3. Chuyển Map thành List và sắp xếp theo ngày
        List<TeacherScheduleResponse> result = new ArrayList<>(eventMap.values());
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
        
        // Lưu studyDays và studyTime tại thời điểm điểm danh để giữ nguyên lịch học gốc
        // (tránh bị thay đổi khi admin sửa lịch lớp sau này)
        if (classEntity.getStudyDays() != null && !classEntity.getStudyDays().isEmpty()) {
            session.setStudyDays(classEntity.getStudyDays().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(",")));
        }
        if (classEntity.getStudyTime() != null) {
            session.setStudyTime(classEntity.getStudyTime().name());
        }
        
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

        // Send notifications to students about attendance status
        for (AttendanceRecord record : records) {
            if (record.getStudent() != null && record.getStudent().getUser() != null) {
                Integer studentUserId = record.getStudent().getUser().getUserId();
                String status = record.getStatus().name();
                String severity = status.equals("PRESENT") ? "low" : "medium";
                String message = status.equals("PRESENT") 
                    ? "Bạn đã có mặt trong buổi học ngày " + savedSession.getAttendanceDate()
                    : "Bạn đã vắng mặt trong buổi học ngày " + savedSession.getAttendanceDate();
                
                notificationService.createAndSend(
                    studentUserId,
                    "ATTENDANCE_RECORDED",
                    "Điểm danh lớp " + classEntity.getName(),
                    message,
                    "attendance_session",
                    savedSession.getSessionId().longValue(),
                    severity
                );
                
                // Check if student has been absent more than 2 times in this class
                List<AttendanceRecord> allStudentAttendance = recordRepo
                    .findByStudentIdAndClassIdOrderByAttendanceDateDesc(
                        record.getStudent().getStudentId(), 
                        request.getClassId()
                    );
                
                long totalAbsences = allStudentAttendance.stream()
                    .filter(ar -> ar.getStatus() == AttendanceStatus.ABSENT)
                    .count();
                
                if (totalAbsences > 2) {
                    notificationService.createAndSend(
                        studentUserId,
                        "ATTENDANCE_WARNING",
                        "Cảnh báo điểm danh",
                        String.format("Bạn đã vắng %d buổi học trong lớp %s. Vui lòng liên hệ giảng viên để được hỗ trợ.", 
                            totalAbsences, classEntity.getName()),
                        "class",
                        classEntity.getClassId().longValue(),
                        "high"
                    );
                }
            }
        }

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
        
        // Send notifications to students whose attendance was updated
        if (request.getRecords() != null && !request.getRecords().isEmpty()) {
            for (UpdateAttendanceSessionRequest.UpdateAttendanceRecordRequest recordReq : request.getRecords()) {
                AttendanceRecord record = updatedSession.getRecords().stream()
                        .filter(r -> r.getRecordId().equals(recordReq.getRecordId()))
                        .findFirst()
                        .orElse(null);
                
                if (record != null && record.getStudent() != null && record.getStudent().getUser() != null) {
                    Integer studentUserId = record.getStudent().getUser().getUserId();
                    String status = record.getStatus().name();
                    String severity = status.equals("PRESENT") ? "low" : "medium";
                    String message = status.equals("PRESENT") 
                        ? "Điểm danh của bạn đã được cập nhật: Có mặt - ngày " + updatedSession.getAttendanceDate()
                        : "Điểm danh của bạn đã được cập nhật: Vắng mặt - ngày " + updatedSession.getAttendanceDate();
                    
                    notificationService.createAndSend(
                        studentUserId,
                        "ATTENDANCE_UPDATED",
                        "Cập nhật điểm danh - " + updatedSession.getClassEntity().getName(),
                        message,
                        "attendance_session",
                        updatedSession.getSessionId().longValue(),
                        severity
                    );
                    
                    // Check if student has been absent more than 2 times in this class
                    List<AttendanceRecord> allStudentAttendance = recordRepo
                        .findByStudentIdAndClassIdOrderByAttendanceDateDesc(
                            record.getStudent().getStudentId(), 
                            updatedSession.getClassEntity().getClassId()
                        );
                    
                    long absentCount = allStudentAttendance.stream()
                        .filter(ar -> ar.getStatus() == AttendanceStatus.ABSENT)
                        .count();
                    
                    if (absentCount > 2) {
                        notificationService.createAndSend(
                            studentUserId,
                            "ATTENDANCE_WARNING",
                            "Cảnh báo điểm danh",
                            String.format("Bạn đã vắng %d buổi học trong lớp %s. Vui lòng liên hệ giảng viên để được hỗ trợ.", 
                                absentCount, updatedSession.getClassEntity().getName()),
                            "class",
                            updatedSession.getClassEntity().getClassId().longValue(),
                            "high"
                        );
                    }
                }
            }
        }
        
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

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceHistoryResponse getMyAttendanceByClass(Integer currentUserId, Integer classId) {
        // Tìm Student từ userId
        Student student = studentRepo.findByUserIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new NotFoundException("Student not found for userId: " + currentUserId));
        
        // Gọi method getStudentAttendanceHistory với studentId
        return getStudentAttendanceHistory(student.getStudentId(), classId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceHistoryResponse getStudentAttendanceHistory(Integer studentId, Integer classId) {
        // Validate student exists
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        // Validate class exists
        ClassEntity classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId));

        // Validate student has at least one enrollment (active or revoked) in this class
        List<Enrollment> enrollments = enrollmentRepo.findByStudent_StudentIdAndClassEntity_ClassId(studentId, classId);
        if (enrollments.isEmpty()) {
            throw new NotFoundException("Student is not enrolled in this class");
        }

        // Get ALL attendance records for this student in this class
        // (không phụ thuộc vào enrollment_id cụ thể)
        List<AttendanceRecord> records = recordRepo.findByStudentIdAndClassIdOrderByAttendanceDateDesc(
                studentId, classId);

        // Calculate statistics
        int totalSessions = records.size();
        int presentCount = 0;
        int absentCount = 0;

        for (AttendanceRecord record : records) {
            if (record.getStatus() == AttendanceStatus.PRESENT) {
                presentCount++;
            } else {
                absentCount++;
            }
        }

        // Map to detailed records
        List<StudentAttendanceHistoryResponse.AttendanceDetail> details = new ArrayList<>();
        for (AttendanceRecord record : records) {
            StudentAttendanceHistoryResponse.AttendanceDetail detail = 
                new StudentAttendanceHistoryResponse.AttendanceDetail();
            detail.setSessionId(record.getSession().getSessionId());
            detail.setAttendanceDate(record.getSession().getAttendanceDate());
            detail.setStatus(record.getStatus());
            detail.setNotes(record.getNotes());
            detail.setTeacherName(record.getSession().getTeacher().getFullName());
            details.add(detail);
        }

        // Build response
        StudentAttendanceHistoryResponse response = new StudentAttendanceHistoryResponse();
        response.setStudentId(student.getStudentId());
        response.setStudentName(student.getFullName());
        response.setStudentCode("SV" + String.format("%03d", student.getStudentId()));
        response.setClassId(classEntity.getClassId());
        response.setClassName(classEntity.getName());
        response.setTotalSessions(totalSessions);
        response.setPresentCount(presentCount);
        response.setAbsentCount(absentCount);
        response.setRecords(details);
        
        return response;
    }
}

