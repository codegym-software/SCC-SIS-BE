package com.example.sis.services.impl;

import com.example.sis.dtos.student.CreateStudentRequest;
import com.example.sis.dtos.student.StudentResponse;
import com.example.sis.dtos.student.StudentWithEnrollmentsResponse;
import com.example.sis.dtos.student.UpdateStudentRequest;
import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.enums.GenderType;
import com.example.sis.enums.OverallStatus;
import com.example.sis.models.Enrollment;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.StudentService;
import com.example.sis.keycloak.KeycloakAdminClient;
import com.example.sis.models.Role;
import com.example.sis.models.UserRole;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository studentRepo;
    private final UserRepository userRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final KeycloakAdminClient kcAdmin;
    private final RoleRepository roleRepo;
    private final UserRoleRepository userRoleRepo;
    private final com.example.sis.repositories.AttendanceRecordRepository attendanceRecordRepo;
    private final com.example.sis.repositories.GradeRecordRepository gradeRecordRepo;

    @Value("${keycloak.admin.default-temp-password:Team5@12345}")
    private String defaultTempPassword;

    public StudentServiceImpl(StudentRepository studentRepo, 
                             UserRepository userRepo,
                             EnrollmentRepository enrollmentRepo,
                             KeycloakAdminClient kcAdmin,
                             RoleRepository roleRepo,
                             UserRoleRepository userRoleRepo,
                             com.example.sis.repositories.AttendanceRecordRepository attendanceRecordRepo,
                             com.example.sis.repositories.GradeRecordRepository gradeRecordRepo) {
        this.studentRepo = studentRepo;
        this.userRepo = userRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.kcAdmin = kcAdmin;
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.attendanceRecordRepo = attendanceRecordRepo;
        this.gradeRecordRepo = gradeRecordRepo;
    }

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, Integer createdByUserId) {

        // 1. Validate email không trùng (cả students và users) - case-insensitive
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (studentRepo.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email \"" + request.getEmail() + "\" đã được sử dụng bởi một học viên khác trong hệ thống. Vui lòng sử dụng email khác.");
        }
        if (userRepo.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email \"" + request.getEmail() + "\" đã được sử dụng bởi một người dùng khác trong hệ thống. Vui lòng sử dụng email khác.");
        }

        // 2. Tạo Student entity
        Student student = new Student();
        student.setFullName(request.getFullName());
        student.setEmail(normalizedEmail); // Lưu email đã normalize (lowercase)
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
        // Mặc định là PENDING khi chưa có lớp, sẽ chuyển thành ACTIVE khi được gán vào lớp
        student.setOverallStatus(OverallStatus.PENDING);

        // Set audit fields
        if (createdByUserId != null) {
            User createdByUser = userRepo.findById(createdByUserId).orElse(null);
            if (createdByUser != null) {
                student.setCreatedBy(createdByUser);
                student.setUpdatedBy(createdByUser);
            }
        }

        // ========== TẠO TÀI KHOẢN USER + KEYCLOAK ==========
        try {
            log.info("🔐 Bắt đầu tạo tài khoản đăng nhập cho học viên: {}", request.getEmail());

            // 3. Tạo user trên Keycloak
            String username = normalizedEmail; // Dùng email đã normalize làm username
            String[] names = splitName(request.getFullName());
            String keycloakUserId = kcAdmin.createUser(
                username, 
                normalizedEmail, 
                names[0],  // firstName
                names[1],  // lastName
                true       // enabled
            );
            
            log.info("✅ Đã tạo user trên Keycloak - ID: {}", keycloakUserId);

            // 4. Set mật khẩu tạm
            if (defaultTempPassword != null && !defaultTempPassword.isBlank()) {
                try {
                    kcAdmin.setTemporaryPassword(keycloakUserId, defaultTempPassword, true);
                    log.info("✅ Đã set mật khẩu tạm cho user: {}", keycloakUserId);
                } catch (Exception pwEx) {
                    log.warn("⚠️ Không set được mật khẩu tạm: {}", pwEx.getMessage());
                }
            }

            // 5. Tạo User entity trong DB
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(normalizedEmail); // Lưu email đã normalize (lowercase)
            user.setPhone(request.getPhone());
            user.setKeycloakUserId(keycloakUserId);
            user.setDob(request.getDob());
            if (request.getGender() != null && !request.getGender().isBlank()) {
                user.setGender(GenderType.valueOf(request.getGender().toUpperCase()));
            }
            user.setNationalIdNo(request.getNationalIdNo());
            user.setAddressLine(request.getAddressLine());
            user.setProvince(request.getProvince());
            user.setDistrict(request.getDistrict());
            user.setWard(request.getWard());
            user.setActive(true);
            
            user = userRepo.save(user);
            log.info("✅ Đã tạo User trong DB - User ID: {}", user.getUserId());

            // 6. Gán role STUDENT cho user
            Optional<Integer> studentRoleIdOpt = roleRepo.findIdByCode("STUDENT");
            if (studentRoleIdOpt.isPresent()) {
                Role studentRole = roleRepo.findById(studentRoleIdOpt.get()).orElse(null);
                if (studentRole != null) {
                    UserRole userRole = new UserRole();
                    userRole.setUser(user);
                    userRole.setRole(studentRole);
                    userRole.setCenter(null); // STUDENT role không cần center
                    userRole.setAssignedAt(java.time.LocalDateTime.now());
                    userRoleRepo.save(userRole);
                    log.info("✅ Đã gán role STUDENT cho User ID: {}", user.getUserId());
                } else {
                    log.warn("⚠️ Không tìm thấy role STUDENT để gán");
                }
            } else {
                log.warn("⚠️ Không tìm thấy role code STUDENT trong hệ thống");
            }

            // 7. Liên kết Student với User
            student.setUser(user);
            log.info("✅ Đã liên kết Student với User account");

        } catch (IllegalArgumentException e) {
            // Lỗi validation (email trùng trong Keycloak) - re-throw để GlobalExceptionHandler xử lý
            log.error("❌ Email đã tồn tại trong Keycloak: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Các lỗi khác (network, Keycloak server error, ...)
            log.error("❌ Lỗi khi tạo tài khoản user cho học viên: {}", e.getMessage(), e);
            throw new IllegalStateException("Không thể tạo tài khoản đăng nhập cho học viên: " + e.getMessage());
        }
        // ========== KẾT THÚC TẠO TÀI KHOẢN ==========

        // 8. Lưu Student vào database
        student = studentRepo.save(student);
        log.info("✅ Đã tạo hồ sơ học viên - Student ID: {} - {} (User ID: {})", 
            student.getStudentId(), student.getFullName(), 
            student.getUser() != null ? student.getUser().getUserId() : "N/A");

        // 9. Trả về response
        return toResponse(student);
    }

    /**
     * Helper method: Tách tên thành firstName và lastName
     */
    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return new String[]{parts[0], ""};
        String lastName = parts[parts.length - 1];
        String firstName = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
        return new String[]{firstName, lastName};
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
    public byte[] exportStudentsToExcel(String status) throws IOException {
        // Lấy danh sách students theo status (nếu có)
        List<Student> students;
        if (status != null && !status.isEmpty()) {
            // Lọc theo overallStatus
            students = studentRepo.findAllActiveStudents().stream()
                    .filter(s -> s.getOverallStatus() != null && s.getOverallStatus().name().equals(status))
                    .collect(Collectors.toList());
        } else {
            // Lấy tất cả
            students = studentRepo.findAllActiveStudents();
        }

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
    public byte[] generateImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Student Import Template");

            // Create header style (bold + blue background)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            
            // Create date cell style for example row
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            short df = creationHelper.createDataFormat().getFormat("yyyy-mm-dd");
            dateStyle.setDataFormat(df);

            // Header row - must match import column order (without Student ID)
            Row headerRow = sheet.createRow(0);
            String[] columns = new String[] {
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
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // Example row with sample data
            Row exampleRow = sheet.createRow(1);
            int c = 0;
            exampleRow.createCell(c++).setCellValue("Nguyen Van A");
            exampleRow.createCell(c++).setCellValue("nguyenvana@example.com");
            exampleRow.createCell(c++).setCellValue("0901234567");
            
            Cell dobCell = exampleRow.createCell(c++);
            dobCell.setCellValue("2000-01-15");
            
            exampleRow.createCell(c++).setCellValue("MALE");
            exampleRow.createCell(c++).setCellValue("001234567890");
            exampleRow.createCell(c++).setCellValue("123 ABC Street");
            exampleRow.createCell(c++).setCellValue("Ha Noi");
            exampleRow.createCell(c++).setCellValue("Cau Giay");
            exampleRow.createCell(c++).setCellValue("Dich Vong");
            exampleRow.createCell(c++).setCellValue("Sample student");

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
                    // Read cells by column index - starts from col 0 (no Student ID in template)
                    int c = 0;

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

        // 3. Set deletedAt = hiện tại VÀ đổi trạng thái sang DROPPED
        student.setDeletedAt(java.time.LocalDateTime.now());
        student.setOverallStatus(OverallStatus.DROPPED);

        // 4. Lưu vào database
        studentRepo.save(student);
        log.info("✅ Đã xóa mềm học viên (status -> DROPPED) - Student ID: {} - {}", student.getStudentId(), student.getFullName());
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
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận: PENDING, ACTIVE, DROPPED, GRADUATED");
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

        // 5. Nếu chuyển sang DROPPED, cập nhật tất cả enrollment sang DROPPED
        if (newStatus == OverallStatus.DROPPED) {
            List<Enrollment> activeEnrollments = enrollmentRepo.findByStudent_StudentIdAndRevokedAtIsNull(studentId);
            
            for (Enrollment enrollment : activeEnrollments) {
                // Chỉ cập nhật các enrollment đang ACTIVE hoặc SUSPENDED
                if (enrollment.getStatus() == EnrollmentStatus.ACTIVE || 
                    enrollment.getStatus() == EnrollmentStatus.SUSPENDED) {
                    
                    enrollment.setStatus(EnrollmentStatus.DROPPED);
                    enrollment.setLeftAt(LocalDate.now());
                    enrollment.setUpdatedAt(LocalDateTime.now());
                    
                    // Thêm ghi chú ngày tháng
                    String note = "Note: [" + LocalDate.now() + "]";
                    if (enrollment.getNote() != null && !enrollment.getNote().isEmpty()) {
                        enrollment.setNote(enrollment.getNote() + "\n" + note);
                    } else {
                        enrollment.setNote(note);
                    }
                    
                    enrollmentRepo.save(enrollment);
                    log.info("📝 Đã cập nhật enrollment {} của lớp {} sang DROPPED", 
                            enrollment.getEnrollmentId(), 
                            enrollment.getClassEntity().getClassId());
                }
            }
            
            log.info("✅ Đã cập nhật {} enrollment(s) sang DROPPED cho học viên ID: {}", 
                    activeEnrollments.size(), studentId);
        }

        // 6. Lưu vào database
        student = studentRepo.save(student);
        log.info("✅ Đã cập nhật trạng thái học viên - Student ID: {} - {}", student.getStudentId(), student.getFullName());

        // 7. Trả về response
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
        response.setUserId(student.getUser() != null ? student.getUser().getUserId() : null);
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentWithEnrollmentsResponse getStudentWithEnrollmentsById(Integer studentId) {
        log.info("📋 Lấy thông tin học viên với enrollments ID: {}", studentId);

        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với ID: " + studentId));

        return toStudentWithEnrollmentsResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentWithEnrollmentsResponse> getAllStudentsWithEnrollments() {
        log.info("📋 Lấy danh sách tất cả học viên với enrollments (chưa bị xóa mềm)");

        return studentRepo.findAllActiveStudents().stream()
                .map(this::toStudentWithEnrollmentsResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Student entity to StudentWithEnrollmentsResponse
     */
    private StudentWithEnrollmentsResponse toStudentWithEnrollmentsResponse(Student student) {
        StudentWithEnrollmentsResponse response = new StudentWithEnrollmentsResponse();
        
        // Basic student info
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
        response.setUserId(student.getUser() != null ? student.getUser().getUserId() : null);
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());

        // Load enrollments using native query or repository method
        List<StudentWithEnrollmentsResponse.EnrollmentDetail> enrollments = 
            studentRepo.findEnrollmentsByStudentId(student.getStudentId()).stream()
                .map(this::mapToEnrollmentDetail)
                .collect(Collectors.toList());
        
        response.setEnrollments(enrollments);
        return response;
    }

    /**
     * Map enrollment data to EnrollmentDetail
     */
    private StudentWithEnrollmentsResponse.EnrollmentDetail mapToEnrollmentDetail(Object[] enrollmentData) {
        return new StudentWithEnrollmentsResponse.EnrollmentDetail(
            (Integer) enrollmentData[0], // enrollmentId
            (Integer) enrollmentData[1], // classId
            (String) enrollmentData[2],  // className
            (String) enrollmentData[3],  // programName
            (String) enrollmentData[4],  // status
            (LocalDate) enrollmentData[5], // enrolledAt
            (LocalDate) enrollmentData[6], // leftAt
            (String) enrollmentData[7]   // enrollmentNote
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getAllStudentWarnings(Integer centerId) {
        List<java.util.Map<String, Object>> allWarnings = new ArrayList<>();
        
        // Get all active students
        List<Student> students = studentRepo.findAllActiveStudents();
        
        for (Student student : students) {
            // Get all active enrollments for this student
            List<Enrollment> activeEnrollments = enrollmentRepo
                .findByStudent_StudentIdAndStatusAndRevokedAtIsNull(
                    student.getStudentId(), 
                    EnrollmentStatus.ACTIVE
                );

            for (Enrollment enrollment : activeEnrollments) {
                Integer classId = enrollment.getClassEntity().getClassId();
                Integer classCenterId = enrollment.getClassEntity().getCenter() != null 
                    ? enrollment.getClassEntity().getCenter().getCenterId() : null;
                
                // Filter by centerId if provided
                if (centerId != null && !centerId.equals(classCenterId)) {
                    continue;
                }

                String className = enrollment.getClassEntity().getName();
                String programName = enrollment.getClassEntity().getProgram() != null 
                    ? enrollment.getClassEntity().getProgram().getName() : "";

                // Count absences for this class
                List<com.example.sis.models.AttendanceRecord> attendanceRecords = 
                    attendanceRecordRepo.findByStudentIdAndClassIdOrderByAttendanceDateDesc(
                        student.getStudentId(), classId);
                
                long absentCount = attendanceRecords.stream()
                    .filter(ar -> ar.getStatus() == com.example.sis.enums.AttendanceStatus.ABSENT)
                    .count();

                // Count failed tests for this class
                List<com.example.sis.models.GradeRecord> gradeRecords = 
                    gradeRecordRepo.findByStudentIdAndClassId(student.getStudentId(), classId);
                
                long failCount = gradeRecords.stream()
                    .filter(gr -> gr.getPassStatus() == com.example.sis.enums.PassStatus.FAIL)
                    .count();

                // Add warning if absences > 2 or failures > 2
                if (absentCount > 2 || failCount > 2) {
                    java.util.Map<String, Object> warning = new java.util.HashMap<>();
                    warning.put("studentId", student.getStudentId());
                    warning.put("code", student.getEmail()); // Use email as code identifier
                    warning.put("name", student.getFullName());
                    warning.put("classCode", enrollment.getClassEntity().getName()); // Use name as classCode
                    warning.put("program", programName);
                    warning.put("severity", absentCount > 2 && failCount > 2 ? "HIGH" : "MEDIUM");
                    
                    // Build detail string
                    List<String> details = new ArrayList<>();
                    if (absentCount > 2) {
                        details.add("Vắng " + absentCount + " buổi");
                    }
                    if (failCount > 2) {
                        details.add("Trượt " + failCount + " bài");
                    }
                    warning.put("detail", String.join(", ", details));
                    
                    // Build reason string
                    if (absentCount > 2 && failCount > 2) {
                        warning.put("reason", "Vắng mặt và học tập kém");
                    } else if (absentCount > 2) {
                        warning.put("reason", "Vắng mặt");
                    } else {
                        warning.put("reason", "Học tập kém");
                    }
                    
                    allWarnings.add(warning);
                }
            }
        }

        return allWarnings;
    }

    @Override
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getStudentWarnings(Integer userId) {
        // Find student by userId
        Student student = studentRepo.findByUserIdAndDeletedAtIsNull(userId)
            .orElse(null);
        
        if (student == null) {
            return new ArrayList<>();
        }

        List<java.util.Map<String, Object>> warnings = new ArrayList<>();
        
        // Get all active enrollments for this student
        List<Enrollment> activeEnrollments = enrollmentRepo
            .findByStudent_StudentIdAndStatusAndRevokedAtIsNull(
                student.getStudentId(), 
                EnrollmentStatus.ACTIVE
            );

        for (Enrollment enrollment : activeEnrollments) {
            Integer classId = enrollment.getClassEntity().getClassId();
            String className = enrollment.getClassEntity().getName();
            String programName = enrollment.getClassEntity().getProgram() != null 
                ? enrollment.getClassEntity().getProgram().getName() : "";

            // Count absences for this class
            List<com.example.sis.models.AttendanceRecord> attendanceRecords = 
                attendanceRecordRepo.findByStudentIdAndClassIdOrderByAttendanceDateDesc(
                    student.getStudentId(), classId);
            
            long absentCount = attendanceRecords.stream()
                .filter(ar -> ar.getStatus() == com.example.sis.enums.AttendanceStatus.ABSENT)
                .count();

            // Count failed tests for this class
            List<com.example.sis.models.GradeRecord> gradeRecords = 
                gradeRecordRepo.findByStudentIdAndClassId(student.getStudentId(), classId);
            
            long failCount = gradeRecords.stream()
                .filter(gr -> gr.getPassStatus() == com.example.sis.enums.PassStatus.FAIL)
                .count();

            // Add warning if absences > 2 or failures > 2
            if (absentCount > 2 || failCount > 2) {
                java.util.Map<String, Object> warning = new java.util.HashMap<>();
                warning.put("classId", classId);
                warning.put("className", className);
                warning.put("programName", programName);
                warning.put("absentCount", absentCount);
                warning.put("failCount", failCount);
                warning.put("hasAbsenceWarning", absentCount > 2);
                warning.put("hasFailWarning", failCount > 2);
                warnings.add(warning);
            }
        }

        return warnings;
    }
}
