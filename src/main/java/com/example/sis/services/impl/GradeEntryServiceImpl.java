package com.example.sis.services.impl;

import com.example.sis.dtos.grade.CreateGradeEntryRequest;
import com.example.sis.dtos.grade.GradeEntryDetailResponse;
import com.example.sis.dtos.grade.GradeEntryResponse;
import com.example.sis.dtos.grade.GradeRecordRequest;
import com.example.sis.dtos.grade.GradeRecordResponse;
import com.example.sis.dtos.grade.StudentGradesResponse;
import com.example.sis.dtos.grade.UpdateGradeRecordsRequest;
import com.example.sis.dtos.module.ModuleResponse;
import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.Enrollment;
import com.example.sis.models.GradeEntry;
import com.example.sis.models.GradeRecord;
import com.example.sis.models.Module;
import com.example.sis.models.Student;
import com.example.sis.models.User;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.GradeEntryRepository;
import com.example.sis.repositories.GradeRecordRepository;
import com.example.sis.repositories.ModuleRepository;
import com.example.sis.repositories.StudentRepository;
import com.example.sis.services.GradeEntryService;
import com.example.sis.services.ModuleService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GradeEntryServiceImpl implements GradeEntryService {

    private final GradeEntryRepository gradeEntryRepository;
    private final GradeRecordRepository gradeRecordRepository;
    private final ClassRepository classRepository;
    private final ModuleRepository moduleRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EntityManager entityManager;
    private final ModuleService moduleService;

    public GradeEntryServiceImpl(
            GradeEntryRepository gradeEntryRepository,
            GradeRecordRepository gradeRecordRepository,
            ClassRepository classRepository,
            ModuleRepository moduleRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            EntityManager entityManager,
            ModuleService moduleService) {
        this.gradeEntryRepository = gradeEntryRepository;
        this.gradeRecordRepository = gradeRecordRepository;
        this.classRepository = classRepository;
        this.moduleRepository = moduleRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.entityManager = entityManager;
        this.moduleService = moduleService;
    }

    @Override
    public GradeEntryDetailResponse createGradeEntry(CreateGradeEntryRequest request, Integer currentUserId) {
        // 1. Validate class tồn tại
        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));

        // 2. Validate module tồn tại
        Module module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new NotFoundException("Module not found: " + request.getModuleId()));

        // 3. Validate module thuộc program của class (trigger sẽ check, nhưng check trước để có error message rõ ràng)
        if (!module.getProgramId().equals(classEntity.getProgram().getProgramId())) {
            throw new BadRequestException(
                    "Module must belong to the same program as the class. " +
                            "Class program: " + classEntity.getProgram().getProgramId() +
                            ", Module program: " + module.getProgramId());
        }

        // 4. Validate không tạo trùng grade entry cho cùng class, module, entryDate
        if (gradeEntryRepository.existsByClassEntity_ClassIdAndModule_ModuleIdAndEntryDate(
                request.getClassId(), request.getModuleId(), request.getEntryDate())) {
            throw new BadRequestException(
                    "Grade entry already exists for class " + request.getClassId() +
                            ", module " + request.getModuleId() +
                            " on date " + request.getEntryDate());
        }

        // 5. Validate và lấy danh sách học viên ACTIVE trong lớp
        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(
                        request.getClassId(), EnrollmentStatus.ACTIVE);

        // Tạo map studentId -> enrollment để validate nhanh
        var enrolledStudentIds = activeEnrollments.stream()
                .map(e -> e.getStudent().getStudentId())
                .collect(Collectors.toSet());

        // 6. Validate tất cả studentId trong request phải là học viên đã enroll vào lớp
        List<Integer> invalidStudentIds = new ArrayList<>();
        for (GradeRecordRequest recordRequest : request.getGradeRecords()) {
            if (!enrolledStudentIds.contains(recordRequest.getStudentId())) {
                invalidStudentIds.add(recordRequest.getStudentId());
            }
        }

        if (!invalidStudentIds.isEmpty()) {
            throw new BadRequestException(
                    "Students not enrolled in class: " + invalidStudentIds);
        }

        // 7. Validate không có duplicate studentId trong request
        var studentIdCounts = request.getGradeRecords().stream()
                .collect(Collectors.groupingBy(
                        GradeRecordRequest::getStudentId,
                        Collectors.counting()));
        var duplicates = studentIdCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> e.getKey())
                .collect(Collectors.toList());

        if (!duplicates.isEmpty()) {
            throw new BadRequestException(
                    "Duplicate student IDs in request: " + duplicates);
        }

        // 8. Tạo GradeEntry
        GradeEntry gradeEntry = new GradeEntry();
        gradeEntry.setClassEntity(classEntity);
        gradeEntry.setModule(module);
        gradeEntry.setEntryDate(request.getEntryDate());
        gradeEntry.setCreatedBy(entityManager.getReference(User.class, currentUserId));
        gradeEntry.setCreatedAt(LocalDateTime.now());
        gradeEntry.setUpdatedAt(LocalDateTime.now());
        gradeEntry = gradeEntryRepository.save(gradeEntry);

        // 9. Tạo GradeRecords
        List<GradeRecord> gradeRecords = new ArrayList<>();
        for (GradeRecordRequest recordRequest : request.getGradeRecords()) {
            Student student = studentRepository.findById(recordRequest.getStudentId())
                    .orElseThrow(() -> new NotFoundException(
                            "Student not found: " + recordRequest.getStudentId()));

            GradeRecord gradeRecord = new GradeRecord();
            gradeRecord.setGradeEntry(gradeEntry);
            gradeRecord.setStudent(student);
            gradeRecord.setTheoryScore(recordRequest.getTheoryScore());
            gradeRecord.setPracticeScore(recordRequest.getPracticeScore());
            gradeRecord.setCreatedAt(LocalDateTime.now());
            gradeRecord.setUpdatedAt(LocalDateTime.now());

            gradeRecords.add(gradeRecordRepository.save(gradeRecord));
        }

        // 10. Fetch lại để lấy finalScore và passStatus từ generated columns
        gradeEntry = gradeEntryRepository.findById(gradeEntry.getGradeEntryId())
                .orElseThrow(() -> new NotFoundException("Grade entry not found after creation"));

        return toDetailResponse(gradeEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeEntryResponse> getGradeEntriesByClass(
            Integer classId, Integer moduleId, LocalDate entryDate) {

        // Validate class tồn tại
        if (!classRepository.existsById(classId)) {
            throw new NotFoundException("Class not found: " + classId);
        }

        List<GradeEntry> gradeEntries;
        if (moduleId != null && entryDate != null) {
            // Filter theo cả module và entryDate
            gradeEntries = gradeEntryRepository
                    .findByClassEntity_ClassIdAndModule_ModuleIdOrderByEntryDateDesc(classId, moduleId)
                    .stream()
                    .filter(ge -> ge.getEntryDate().equals(entryDate))
                    .collect(Collectors.toList());
        } else if (moduleId != null) {
            // Filter chỉ theo module
            gradeEntries = gradeEntryRepository
                    .findByClassEntity_ClassIdAndModule_ModuleIdOrderByEntryDateDesc(classId, moduleId);
        } else if (entryDate != null) {
            // Filter chỉ theo entryDate
            gradeEntries = gradeEntryRepository
                    .findByClassEntity_ClassIdOrderByEntryDateDesc(classId)
                    .stream()
                    .filter(ge -> ge.getEntryDate().equals(entryDate))
                    .collect(Collectors.toList());
        } else {
            // Lấy tất cả
            gradeEntries = gradeEntryRepository
                    .findByClassEntity_ClassIdOrderByEntryDateDesc(classId);
        }

        return gradeEntries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGradesResponse getStudentGrades(Integer classId, Integer semester, Integer moduleId) {
        // 1. Validate class tồn tại
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId));

        // 2. Validate module thuộc program của class
        Integer programId = classEntity.getProgram().getProgramId();

        // 3. Tạo response object
        StudentGradesResponse response = new StudentGradesResponse();
        response.setClassId(classId);
        response.setClassName(classEntity.getName());
        response.setSemester(semester);

        // 4. Nếu chưa có moduleId: trả về danh sách modules theo semester
        if (moduleId == null) {
            List<ModuleResponse> modules = moduleService.getModulesBySemester(programId, semester);
            response.setModules(modules);
            response.setGradeRecords(null);
            return response;
        }

        // 5. Nếu có moduleId: validate module và trả về danh sách grade records
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module not found: " + moduleId));

        // Validate module thuộc program và semester đúng
        if (!module.getProgramId().equals(programId)) {
            throw new BadRequestException(
                    "Module must belong to the same program as the class. " +
                            "Class program: " + programId +
                            ", Module program: " + module.getProgramId());
        }

        if (module.getSemester() != null && !module.getSemester().equals(semester)) {
            throw new BadRequestException(
                    "Module semester (" + module.getSemester() + 
                    ") does not match requested semester (" + semester + ")");
        }

        response.setModuleId(moduleId);

        // 6. Lấy tất cả grade entries của class và module (đã sắp xếp theo entryDate DESC)
        List<GradeEntry> gradeEntries = gradeEntryRepository
                .findByClassEntity_ClassIdAndModule_ModuleIdOrderByEntryDateDesc(classId, moduleId);

        // 7. Lấy TẤT CẢ grade records từ TẤT CẢ grade entries
        // Frontend cần tất cả records để có thể group theo entryDate và hiển thị dropdown
        List<GradeRecordResponse> gradeRecords = new ArrayList<>();
        
        // Duyệt qua tất cả grade entries và lấy tất cả records
        for (GradeEntry gradeEntry : gradeEntries) {
            List<GradeRecord> records = gradeRecordRepository
                    .findByGradeEntry_GradeEntryIdOrderByStudent_FullName(gradeEntry.getGradeEntryId());
            
            // Convert tất cả records thành response (bao gồm entryDate)
            for (GradeRecord record : records) {
                gradeRecords.add(toRecordResponse(record));
            }
        }

        // 8. Sắp xếp: đầu tiên theo entryDate (mới nhất trước), sau đó theo tên học viên
        gradeRecords.sort((a, b) -> {
            // So sánh entryDate trước (nếu có)
            String entryDateA = a.getEntryDate() != null ? a.getEntryDate() : "";
            String entryDateB = b.getEntryDate() != null ? b.getEntryDate() : "";
            int dateCompare = entryDateB.compareTo(entryDateA); // DESC: mới nhất trước
            if (dateCompare != 0) {
                return dateCompare;
            }
            // Nếu entryDate giống nhau, sắp xếp theo tên học viên
            String nameA = a.getStudentName() != null ? a.getStudentName() : "";
            String nameB = b.getStudentName() != null ? b.getStudentName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });

        response.setGradeRecords(gradeRecords);
        response.setModules(null);
        return response;
    }

    @Override
    public void deleteGradeEntry(Integer classId, Integer moduleId, LocalDate entryDate) {
        // 1. Tìm grade entry
        GradeEntry gradeEntry = gradeEntryRepository
                .findByClassEntity_ClassIdAndModule_ModuleIdAndEntryDate(classId, moduleId, entryDate)
                .orElseThrow(() -> new NotFoundException(
                        "Grade entry not found for class " + classId +
                        ", module " + moduleId +
                        ", entryDate " + entryDate));

        // 2. Xóa grade entry (cascade sẽ xóa các grade records)
        gradeEntryRepository.delete(gradeEntry);
    }

    @Override
    public GradeEntryDetailResponse updateGradeRecords(
            UpdateGradeRecordsRequest request, Integer currentUserId) {
        
        // 1. Validate class tồn tại
        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));

        // 2. Validate module tồn tại
        Module module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new NotFoundException("Module not found: " + request.getModuleId()));

        // 3. Validate module thuộc program của class
        if (!module.getProgramId().equals(classEntity.getProgram().getProgramId())) {
            throw new BadRequestException(
                    "Module must belong to the same program as the class. " +
                            "Class program: " + classEntity.getProgram().getProgramId() +
                            ", Module program: " + module.getProgramId());
        }

        // 4. Tìm grade entry
        GradeEntry gradeEntry = gradeEntryRepository
                .findByClassEntity_ClassIdAndModule_ModuleIdAndEntryDate(
                        request.getClassId(), request.getModuleId(), request.getEntryDate())
                .orElseThrow(() -> new NotFoundException(
                        "Grade entry not found for class " + request.getClassId() +
                        ", module " + request.getModuleId() +
                        ", entryDate " + request.getEntryDate()));

        // 5. Validate và lấy danh sách học viên ACTIVE trong lớp
        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(
                        request.getClassId(), EnrollmentStatus.ACTIVE);

        var enrolledStudentIds = activeEnrollments.stream()
                .map(e -> e.getStudent().getStudentId())
                .collect(Collectors.toSet());

        // 6. Validate tất cả studentId trong request phải là học viên đã enroll vào lớp
        List<Integer> invalidStudentIds = new ArrayList<>();
        for (GradeRecordRequest recordRequest : request.getGradeRecords()) {
            if (!enrolledStudentIds.contains(recordRequest.getStudentId())) {
                invalidStudentIds.add(recordRequest.getStudentId());
            }
        }

        if (!invalidStudentIds.isEmpty()) {
            throw new BadRequestException(
                    "Students not enrolled in class: " + invalidStudentIds);
        }

        // 7. Validate không có duplicate studentId trong request
        var studentIdCounts = request.getGradeRecords().stream()
                .collect(Collectors.groupingBy(
                        GradeRecordRequest::getStudentId,
                        Collectors.counting()));
        var duplicates = studentIdCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> e.getKey())
                .collect(Collectors.toList());

        if (!duplicates.isEmpty()) {
            throw new BadRequestException(
                    "Duplicate student IDs in request: " + duplicates);
        }

        // 8. Lấy danh sách grade records hiện tại
        List<GradeRecord> existingRecords = gradeRecordRepository
                .findByGradeEntry_GradeEntryIdOrderByStudent_FullName(gradeEntry.getGradeEntryId());

        // Tạo map studentId -> GradeRecord để update nhanh
        var existingRecordsMap = existingRecords.stream()
                .collect(Collectors.toMap(
                        gr -> gr.getStudent().getStudentId(),
                        gr -> gr));

        // 9. Update hoặc create grade records
        List<GradeRecord> updatedRecords = new ArrayList<>();
        for (GradeRecordRequest recordRequest : request.getGradeRecords()) {
            Student student = studentRepository.findById(recordRequest.getStudentId())
                    .orElseThrow(() -> new NotFoundException(
                            "Student not found: " + recordRequest.getStudentId()));

            GradeRecord gradeRecord = existingRecordsMap.get(recordRequest.getStudentId());
            if (gradeRecord == null) {
                // Tạo mới nếu chưa có
                gradeRecord = new GradeRecord();
                gradeRecord.setGradeEntry(gradeEntry);
                gradeRecord.setStudent(student);
                gradeRecord.setCreatedAt(LocalDateTime.now());
            }
            
            // Update điểm
            gradeRecord.setTheoryScore(recordRequest.getTheoryScore());
            gradeRecord.setPracticeScore(recordRequest.getPracticeScore());
            gradeRecord.setUpdatedAt(LocalDateTime.now());
            
            updatedRecords.add(gradeRecordRepository.save(gradeRecord));
        }

        // 10. Xóa các records không có trong request (optional - có thể giữ lại nếu muốn)
        // Nhưng theo logic thông thường, khi update thì nên chỉ giữ lại những records trong request
        var requestStudentIds = request.getGradeRecords().stream()
                .map(GradeRecordRequest::getStudentId)
                .collect(Collectors.toSet());
        
        for (GradeRecord existingRecord : existingRecords) {
            if (!requestStudentIds.contains(existingRecord.getStudent().getStudentId())) {
                gradeRecordRepository.delete(existingRecord);
            }
        }

        // 11. Update updatedAt của grade entry
        gradeEntry.setUpdatedAt(LocalDateTime.now());
        gradeEntryRepository.save(gradeEntry);

        // 12. Fetch lại để lấy finalScore và passStatus từ generated columns
        gradeEntry = gradeEntryRepository.findById(gradeEntry.getGradeEntryId())
                .orElseThrow(() -> new NotFoundException("Grade entry not found after update"));

        return toDetailResponse(gradeEntry);
    }

    // ===== Helper methods =====

    private GradeEntryResponse toResponse(GradeEntry ge) {
        GradeEntryResponse response = new GradeEntryResponse();
        response.setGradeEntryId(ge.getGradeEntryId());
        response.setClassId(ge.getClassEntity().getClassId());
        response.setClassName(ge.getClassEntity().getName());
        response.setModuleId(ge.getModule().getModuleId());
        response.setModuleCode(ge.getModule().getCode());
        response.setModuleName(ge.getModule().getName());
        response.setEntryDate(ge.getEntryDate());
        response.setCreatedBy(ge.getCreatedBy().getUserId());
        response.setCreatedByName(ge.getCreatedBy().getFullName());
        response.setCreatedAt(ge.getCreatedAt());
        response.setUpdatedAt(ge.getUpdatedAt());
        return response;
    }

    private GradeEntryDetailResponse toDetailResponse(GradeEntry ge) {
        GradeEntryDetailResponse response = new GradeEntryDetailResponse();
        response.setGradeEntryId(ge.getGradeEntryId());
        response.setClassId(ge.getClassEntity().getClassId());
        response.setClassName(ge.getClassEntity().getName());
        response.setProgramId(ge.getClassEntity().getProgram().getProgramId());
        response.setProgramName(ge.getClassEntity().getProgram().getName());
        response.setModuleId(ge.getModule().getModuleId());
        response.setModuleCode(ge.getModule().getCode());
        response.setModuleName(ge.getModule().getName());
        response.setSemester(ge.getModule().getSemester());
        response.setEntryDate(ge.getEntryDate());
        response.setCreatedBy(ge.getCreatedBy().getUserId());
        response.setCreatedByName(ge.getCreatedBy().getFullName());
        response.setCreatedAt(ge.getCreatedAt());
        response.setUpdatedAt(ge.getUpdatedAt());

        // Lấy danh sách grade records
        List<GradeRecord> records = gradeRecordRepository
                .findByGradeEntry_GradeEntryIdOrderByStudent_FullName(ge.getGradeEntryId());
        response.setGradeRecords(records.stream()
                .map(this::toRecordResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private GradeRecordResponse toRecordResponse(GradeRecord gr) {
        GradeRecordResponse response = new GradeRecordResponse();
        response.setGradeRecordId(gr.getGradeRecordId());
        response.setStudentId(gr.getStudent().getStudentId());
        response.setStudentName(gr.getStudent().getFullName());
        response.setStudentEmail(gr.getStudent().getEmail());
        response.setTheoryScore(gr.getTheoryScore());
        response.setPracticeScore(gr.getPracticeScore());
        response.setFinalScore(gr.getFinalScore());
        response.setPassStatus(gr.getPassStatus() != null ? gr.getPassStatus().name() : null);
        // Set entryDate từ GradeEntry
        if (gr.getGradeEntry() != null && gr.getGradeEntry().getEntryDate() != null) {
            response.setEntryDate(gr.getGradeEntry().getEntryDate().toString());
        }
        return response;
    }

    // ===== Excel Import/Export Methods =====

    @Override
    public GradeEntryDetailResponse importGradesFromExcel(
            MultipartFile file, Integer classId, Integer moduleId, 
            LocalDate entryDate, Integer currentUserId) throws IOException {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // 1. Validate class và module
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId));
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module not found: " + moduleId));

        // 2. Lấy danh sách học viên ACTIVE trong lớp
        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(classId, EnrollmentStatus.ACTIVE);
        
        // Tạo map studentId -> Student để tìm nhanh
        Map<Integer, Student> studentMap = activeEnrollments.stream()
                .map(Enrollment::getStudent)
                .collect(Collectors.toMap(
                    Student::getStudentId, 
                    s -> s,
                    (existing, replacement) -> existing));

        // 3. Đọc Excel file
        List<GradeRecordRequest> gradeRecords = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }

            // Đọc từ dòng 1 (dòng 0 là header)
            // Format: Student ID | Student Name | Theory Score | Practice Score
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    // Cột 0: Student ID
                    Integer studentId = getIntegerCell(row.getCell(0));
                    if (studentId == null) continue;

                    // Tìm student theo ID
                    Student student = studentMap.get(studentId);
                    if (student == null) {
                        // Skip nếu không tìm thấy student
                        continue;
                    }

                    // Cột 2: Theory Score (0-100)
                    BigDecimal theoryScore = getNumericCell(row.getCell(2));
                    // Cột 3: Practice Score (0-100)
                    BigDecimal practiceScore = getNumericCell(row.getCell(3));

                    // Validate điểm trong khoảng 0-100
                    if (theoryScore != null && (theoryScore.compareTo(BigDecimal.ZERO) < 0 || theoryScore.compareTo(new BigDecimal("100")) > 0)) {
                        continue; // Skip dòng có điểm không hợp lệ
                    }
                    if (practiceScore != null && (practiceScore.compareTo(BigDecimal.ZERO) < 0 || practiceScore.compareTo(new BigDecimal("100")) > 0)) {
                        continue; // Skip dòng có điểm không hợp lệ
                    }

                    GradeRecordRequest record = new GradeRecordRequest();
                    record.setStudentId(studentId);
                    record.setTheoryScore(theoryScore);
                    record.setPracticeScore(practiceScore);
                    gradeRecords.add(record);
                } catch (Exception e) {
                    // Skip dòng lỗi
                    continue;
                }
            }
        }

        // 4. Tạo CreateGradeEntryRequest và gọi createGradeEntry
        CreateGradeEntryRequest request = new CreateGradeEntryRequest();
        request.setClassId(classId);
        request.setModuleId(moduleId);
        request.setEntryDate(entryDate);
        request.setGradeRecords(gradeRecords);

        return createGradeEntry(request, currentUserId);
    }

    @Override
    public byte[] generateGradeImportTemplate(Integer classId, Integer moduleId) throws IOException {
        // Lấy danh sách học viên trong lớp
        List<Enrollment> enrollments = enrollmentRepository
                .findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(classId, EnrollmentStatus.ACTIVE);
        
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Grade Import Template");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student ID", "Student Name", "Theory Score (0-100)", "Practice Score (0-100)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // Data rows với danh sách học viên
            int rowNum = 1;
            for (Enrollment enrollment : enrollments) {
                Student student = enrollment.getStudent();
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(student.getStudentId());
                row.createCell(1).setCellValue(student.getFullName());
                // Để trống điểm để user điền
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportGradesToExcel(Integer classId, Integer semester, Integer moduleId, LocalDate entryDate) throws IOException {
        // Validate: nếu đã chọn module thì phải có entryDate
        if (moduleId != null && entryDate == null) {
            throw new IllegalArgumentException("entryDate is required when moduleId is provided");
        }

        StudentGradesResponse response = getStudentGrades(classId, semester, moduleId);
        
        if (response.getGradeRecords() == null || response.getGradeRecords().isEmpty()) {
            throw new IllegalArgumentException("No grade records to export");
        }

        // Filter theo entryDate nếu có
        List<GradeRecordResponse> recordsToExport = response.getGradeRecords();
        if (entryDate != null) {
            String entryDateStr = entryDate.toString();
            recordsToExport = recordsToExport.stream()
                    .filter(record -> entryDateStr.equals(record.getEntryDate()))
                    .collect(Collectors.toList());
            
            if (recordsToExport.isEmpty()) {
                throw new IllegalArgumentException("No grade records found for the selected date");
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Grades Export");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student ID", "Student Name", "Theory Score", "Practice Score", "Final Score", "Pass Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Data rows
            int rowNum = 1;
            for (GradeRecordResponse record : recordsToExport) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getStudentId());
                row.createCell(1).setCellValue(record.getStudentName());
                if (record.getTheoryScore() != null) {
                    row.createCell(2).setCellValue(record.getTheoryScore().doubleValue());
                }
                if (record.getPracticeScore() != null) {
                    row.createCell(3).setCellValue(record.getPracticeScore().doubleValue());
                }
                if (record.getFinalScore() != null) {
                    row.createCell(4).setCellValue(record.getFinalScore().doubleValue());
                }
                row.createCell(5).setCellValue(record.getPassStatus() != null ? record.getPassStatus() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Helper methods để đọc Excel
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
                    String s = String.valueOf((long) d);
                    return s;
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Integer getIntegerCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private BigDecimal getNumericCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return new BigDecimal(value);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}

