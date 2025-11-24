package com.example.sis.services;

import com.example.sis.dtos.attendance.ClassAttendanceStatisticsDTO;
import com.example.sis.dtos.attendance.DailyAttendanceDTO;
import com.example.sis.dtos.attendance.StudentStatisticDTO;
import com.example.sis.enums.AttendanceStatus;
import com.example.sis.models.AttendanceRecord;
import com.example.sis.models.AttendanceSession;
import com.example.sis.models.Enrollment;
import com.example.sis.repositories.AttendanceRecordRepository;
import com.example.sis.repositories.AttendanceSessionRepository;
import com.example.sis.repositories.EnrollmentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassAttendanceStatisticsService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ClassAttendanceStatisticsService(AttendanceSessionRepository attendanceSessionRepository,
                                           AttendanceRecordRepository attendanceRecordRepository,
                                           EnrollmentRepository enrollmentRepository) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Lấy thống kê điểm danh của lớp học theo tháng/năm
     */
    @Transactional(readOnly = true)
    public ClassAttendanceStatisticsDTO getClassAttendanceStatistics(Integer classId, Integer month, Integer year) {
        // Xác định ngày bắt đầu và kết thúc của tháng
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Lấy tất cả buổi điểm danh trong tháng
        List<AttendanceSession> sessions = attendanceSessionRepository
                .findByClassEntity_ClassIdAndAttendanceDateBetweenAndDeletedFalse(classId, startDate, endDate);

        if (sessions.isEmpty()) {
            // Trả về thống kê rỗng nếu không có buổi điểm danh
            return new ClassAttendanceStatisticsDTO(
                    classId,
                    getClassName(classId),
                    month,
                    year,
                    0,
                    0,
                    0.0,
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        // Lấy danh sách học viên trong lớp
        List<Enrollment> enrollments = enrollmentRepository.findByClassEntity_ClassIdAndDeletedFalse(classId);
        
        String className = sessions.get(0).getClassEntity().getName();
        
        // Tính thống kê theo ngày
        List<DailyAttendanceDTO> dailyAttendance = calculateDailyAttendance(sessions);
        
        // Tính thống kê theo học viên
        List<StudentStatisticDTO> studentStatistics = calculateStudentStatistics(sessions, enrollments);
        
        // Tính các chỉ số tổng hợp
        int totalStudents = enrollments.size();
        int totalSessions = sessions.size();
        double averageAttendanceRate = calculateAverageAttendanceRate(studentStatistics);
        int studentsNeedingHelp = (int) studentStatistics.stream()
                .filter(s -> s.getAttendanceRate() < 80.0)
                .count();

        return new ClassAttendanceStatisticsDTO(
                classId,
                className,
                month,
                year,
                totalStudents,
                totalSessions,
                Math.round(averageAttendanceRate * 10.0) / 10.0,
                studentsNeedingHelp,
                dailyAttendance,
                studentStatistics
        );
    }

    /**
     * Tính thống kê điểm danh theo ngày
     */
    private List<DailyAttendanceDTO> calculateDailyAttendance(List<AttendanceSession> sessions) {
        Map<LocalDate, List<AttendanceSession>> sessionsByDate = sessions.stream()
                .collect(Collectors.groupingBy(AttendanceSession::getAttendanceDate));

        return sessionsByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<AttendanceSession> dateSessions = entry.getValue();
                    
                    int sessionCount = dateSessions.size();
                    int totalPresent = 0;
                    int totalAbsent = 0;

                    for (AttendanceSession session : dateSessions) {
                        List<AttendanceRecord> records = attendanceRecordRepository
                                .findBySession_SessionIdAndDeletedFalse(session.getSessionId());
                        
                        totalPresent += (int) records.stream()
                                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
                                .count();
                        totalAbsent += (int) records.stream()
                                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                                .count();
                    }

                    double attendanceRate = (totalPresent + totalAbsent) > 0
                            ? (totalPresent * 100.0) / (totalPresent + totalAbsent)
                            : 0.0;

                    return new DailyAttendanceDTO(
                            date,
                            sessionCount,
                            totalPresent,
                            totalAbsent,
                            Math.round(attendanceRate * 10.0) / 10.0
                    );
                })
                .sorted(Comparator.comparing(DailyAttendanceDTO::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Tính thống kê điểm danh theo học viên
     */
    private List<StudentStatisticDTO> calculateStudentStatistics(
            List<AttendanceSession> sessions, List<Enrollment> enrollments) {
        
        // Lấy tất cả records của các session
        Set<Integer> sessionIds = sessions.stream()
                .map(AttendanceSession::getSessionId)
                .collect(Collectors.toSet());

        List<AttendanceRecord> allRecords = new ArrayList<>();
        for (Integer sessionId : sessionIds) {
            allRecords.addAll(attendanceRecordRepository
                    .findBySession_SessionIdAndDeletedFalse(sessionId));
        }

        // Nhóm records theo studentId
        Map<Integer, List<AttendanceRecord>> recordsByStudent = allRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getStudentId()));

        return enrollments.stream()
                .map(enrollment -> {
                    Integer studentId = enrollment.getStudent().getStudentId();
                    String studentName = enrollment.getStudent().getFullName();
                    String studentCode = String.format("SV%03d", studentId);

                    List<AttendanceRecord> studentRecords = recordsByStudent.getOrDefault(studentId, new ArrayList<>());
                    
                    int totalSessions = sessions.size();
                    long presentCount = studentRecords.stream()
                            .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
                            .count();
                    long absentCount = studentRecords.stream()
                            .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                            .count();

                    double attendanceRate = totalSessions > 0
                            ? (presentCount * 100.0) / totalSessions
                            : 0.0;

                    return new StudentStatisticDTO(
                            studentId,
                            studentName,
                            studentCode,
                            totalSessions,
                            (int) presentCount,
                            (int) absentCount,
                            Math.round(attendanceRate * 10.0) / 10.0
                    );
                })
                .sorted(Comparator.comparing(StudentStatisticDTO::getStudentName))
                .collect(Collectors.toList());
    }

    /**
     * Tính tỷ lệ điểm danh trung bình
     */
    private double calculateAverageAttendanceRate(List<StudentStatisticDTO> studentStatistics) {
        if (studentStatistics.isEmpty()) {
            return 0.0;
        }

        double sum = studentStatistics.stream()
                .mapToDouble(StudentStatisticDTO::getAttendanceRate)
                .sum();

        return sum / studentStatistics.size();
    }

    /**
     * Xuất thống kê ra file Excel
     */
    @Transactional(readOnly = true)
    public byte[] exportToExcel(Integer classId, Integer month, Integer year) throws IOException {
        ClassAttendanceStatisticsDTO statistics = getClassAttendanceStatistics(classId, month, year);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Sheet 1: Tổng quan
            Sheet summarySheet = workbook.createSheet("Tổng quan");
            createSummarySheet(summarySheet, statistics);

            // Sheet 2: Thống kê theo ngày
            Sheet dailySheet = workbook.createSheet("Thống kê theo ngày");
            createDailySheet(dailySheet, statistics);

            // Sheet 3: Thống kê học viên
            Sheet studentSheet = workbook.createSheet("Thống kê học viên");
            createStudentSheet(studentSheet, statistics);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Tạo sheet tổng quan
     */
    private void createSummarySheet(Sheet sheet, ClassAttendanceStatisticsDTO statistics) {
        // Style cho header
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        headerStyle.setFont(headerFont);

        int rowNum = 0;

        // Tiêu đề
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("THỐNG KÊ ĐIỂM DANH LỚP HỌC");
        titleCell.setCellStyle(headerStyle);

        rowNum++; // Dòng trống

        // Thông tin lớp
        createRow(sheet, rowNum++, "Lớp học:", statistics.getClassName());
        createRow(sheet, rowNum++, "Tháng/Năm:", statistics.getMonth() + "/" + statistics.getYear());
        
        rowNum++; // Dòng trống

        // Thống kê tổng quan
        createRow(sheet, rowNum++, "Tổng số học viên:", statistics.getTotalStudents().toString());
        createRow(sheet, rowNum++, "Tổng số buổi học:", statistics.getTotalSessions().toString());
        createRow(sheet, rowNum++, "Tỷ lệ điểm danh trung bình:", 
                String.format("%.1f%%", statistics.getAverageAttendanceRate()));
        createRow(sheet, rowNum++, "Số học viên cần hỗ trợ:", statistics.getStudentsNeedingHelp().toString());

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Tạo sheet thống kê theo ngày
     */
    private void createDailySheet(Sheet sheet, ClassAttendanceStatisticsDTO statistics) {
        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Ngày", "Số buổi", "Có mặt", "Vắng mặt", "Tỷ lệ (%)"};
        
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        for (DailyAttendanceDTO daily : statistics.getDailyAttendance()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(daily.getDate().toString());
            row.createCell(1).setCellValue(daily.getSessionCount());
            row.createCell(2).setCellValue(daily.getPresentCount());
            row.createCell(3).setCellValue(daily.getAbsentCount());
            row.createCell(4).setCellValue(String.format("%.1f%%", daily.getAttendanceRate()));
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Tạo sheet thống kê học viên
     */
    private void createStudentSheet(Sheet sheet, ClassAttendanceStatisticsDTO statistics) {
        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mã học viên", "Tên học viên", "Tổng buổi", "Có mặt", "Vắng", "Tỷ lệ (%)"};
        
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        for (StudentStatisticDTO student : statistics.getStudentStatistics()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(student.getStudentCode());
            row.createCell(1).setCellValue(student.getStudentName());
            row.createCell(2).setCellValue(student.getTotalSessions());
            row.createCell(3).setCellValue(student.getPresentCount());
            row.createCell(4).setCellValue(student.getAbsentCount());
            row.createCell(5).setCellValue(String.format("%.1f%%", student.getAttendanceRate()));
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Tạo style cho header
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Tạo một row với label và value
     */
    private void createRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        
        CellStyle boldStyle = sheet.getWorkbook().createCellStyle();
        Font boldFont = sheet.getWorkbook().createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        labelCell.setCellStyle(boldStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
    }

    /**
     * Lấy tên lớp từ classId
     */
    private String getClassName(Integer classId) {
        return enrollmentRepository.findByClassEntity_ClassIdAndDeletedFalse(classId)
                .stream()
                .findFirst()
                .map(e -> e.getClassEntity().getName())
                .orElse("Unknown Class");
    }
}
