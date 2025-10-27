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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    public byte[] exportStudentsToExcel() throws IOException {
        List<Student> students = studentRepo.findAllActiveStudents();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");

            // Header
            Row header = sheet.createRow(0);
            String[] columns = new String[] {
                    "Student ID",
                    "Full name",
                    "Email",
                    "Phone",
                    "DOB",
                    "Gender",
                    "National ID",
                    "Address",
                    "Province",
                    "District",
                    "Ward",
                    "Note",
                    "Status",
                    "Created At",
                    "Updated At"
            };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
                sheet.autoSizeColumn(i);
            }

            // Date cell style
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            short df = creationHelper.createDataFormat().getFormat("yyyy-mm-dd");
            dateStyle.setDataFormat(df);

            int rowIdx = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowIdx++);

                int c = 0;
                row.createCell(c++).setCellValue(s.getStudentId() != null ? s.getStudentId() : 0);
                row.createCell(c++).setCellValue(s.getFullName() != null ? s.getFullName() : "");
                row.createCell(c++).setCellValue(s.getEmail() != null ? s.getEmail() : "");
                row.createCell(c++).setCellValue(s.getPhone() != null ? s.getPhone() : "");

                Cell dobCell = row.createCell(c++);
                if (s.getDob() != null) {
                    dobCell.setCellValue(java.util.Date.from(s.getDob().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    dobCell.setCellStyle(dateStyle);
                } else {
                    dobCell.setCellValue("");
                }

                row.createCell(c++).setCellValue(s.getGender() != null ? s.getGender().name() : "");
                row.createCell(c++).setCellValue(s.getNationalIdNo() != null ? s.getNationalIdNo() : "");
                row.createCell(c++).setCellValue(s.getAddressLine() != null ? s.getAddressLine() : "");
                row.createCell(c++).setCellValue(s.getProvince() != null ? s.getProvince() : "");
                row.createCell(c++).setCellValue(s.getDistrict() != null ? s.getDistrict() : "");
                row.createCell(c++).setCellValue(s.getWard() != null ? s.getWard() : "");
                row.createCell(c++).setCellValue(s.getNote() != null ? s.getNote() : "");
                row.createCell(c++).setCellValue(s.getOverallStatus() != null ? s.getOverallStatus().name() : "");
                
                Cell createdAt = row.createCell(c++);
                if (s.getCreatedAt() != null) {
                    createdAt.setCellValue(java.util.Date.from(s.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
                    createdAt.setCellStyle(dateStyle);
                } else {
                    createdAt.setCellValue("");
                }

                Cell updatedAt = row.createCell(c++);
                if (s.getUpdatedAt() != null) {
                    updatedAt.setCellValue(java.util.Date.from(s.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()));
                    updatedAt.setCellStyle(dateStyle);
                } else {
                    updatedAt.setCellValue("");
                }
            }

            // Autosize columns (optional)
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");

            // Create header row
            Row header = sheet.createRow(0);
            String[] columns = new String[] {
                    "Student ID",
                    "Full name",
                    "Email",
                    "Phone",
                    "DOB",
                    "Gender",
                    "National ID",
                    "Address",
                    "Province",
                    "District",
                    "Ward",
                    "Note"
            };

            // Style for header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create header cells
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    @Transactional
    public List<StudentResponse> importStudentsFromExcel(MultipartFile file, Integer createdByUserId) throws IOException {
        List<StudentResponse> created = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return created;
            }

            // Assume first row is header. Start from rowIndex = 1
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    // Read cells by column index consistent with export header
                    int c = 0;
                    // Skip Student ID col (col 0)
                    Cell skipId = row.getCell(c++);

                    String fullName = getStringCell(row.getCell(c++));
                    String email = getStringCell(row.getCell(c++));
                    String phone = getStringCell(row.getCell(c++));
                    LocalDate dob = getDateCell(row.getCell(c++));
                    String gender = getStringCell(row.getCell(c++));
                    String nationalId = getStringCell(row.getCell(c++));
                    String address = getStringCell(row.getCell(c++));
                    String province = getStringCell(row.getCell(c++));
                    String district = getStringCell(row.getCell(c++));
                    String ward = getStringCell(row.getCell(c++));
                    String note = getStringCell(row.getCell(c++));
                    // skip status, createdAt, updatedAt columns if present

                    // Minimal validation
                    if (email == null || email.isBlank()) {
                        // skip rows without email
                        continue;
                    }
                    if (fullName == null || fullName.isBlank()) {
                        // skip rows without name
                        continue;
                    }

                    // Create request DTO
                    com.example.sis.dtos.student.CreateStudentRequest req = new com.example.sis.dtos.student.CreateStudentRequest();
                    req.setFullName(fullName);
                    req.setEmail(email);
                    req.setPhone(phone);
                    req.setDob(dob);
                    req.setGender(gender);
                    req.setNationalIdNo(nationalId);
                    req.setAddressLine(address);
                    req.setProvince(province);
                    req.setDistrict(district);
                    req.setWard(ward);
                    req.setNote(note);

                    try {
                        // Reuse existing createStudent (it will validate duplicates)
                        StudentResponse resp = createStudent(req, createdByUserId);
                        created.add(resp);
                    } catch (IllegalArgumentException ex) {
                        // Skip duplicate or invalid row; could collect errors if needed
                        log.warn("Skipping row {} due to validation error: {}", r + 1, ex.getMessage());
                    }
                } catch (Exception rowEx) {
                    log.warn("Failed to parse row {}, skipping. Error: {}", r + 1, rowEx.getMessage());
                }
            }
        }

        return created;
    }

    // Helper: read string cell safely
    private String getStringCell(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return String.valueOf(cell.getLocalDateTimeCellValue().toLocalDate());
                } else {
                    double d = cell.getNumericCellValue();
                    String s = String.valueOf(d);
                    if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
                    return s;
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    // Helper: parse date cell into LocalDate
    private LocalDate getDateCell(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            java.util.Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String s = getStringCell(cell);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
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
