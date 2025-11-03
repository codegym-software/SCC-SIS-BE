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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

        // 7. Lấy grade records từ các grade entries, ưu tiên record mới nhất cho mỗi student
        // Map để track student đã có record chưa (key: studentId, value: GradeRecordResponse)
        java.util.Map<Integer, GradeRecordResponse> studentRecordsMap = new java.util.HashMap<>();
        
        // Duyệt từ entry mới nhất đến cũ nhất, chỉ lấy record của student chưa có
        for (GradeEntry gradeEntry : gradeEntries) {
            List<GradeRecord> records = gradeRecordRepository
                    .findByGradeEntry_GradeEntryIdOrderByStudent_FullName(gradeEntry.getGradeEntryId());
            
            for (GradeRecord record : records) {
                Integer studentId = record.getStudent().getStudentId();
                // Chỉ thêm nếu student chưa có record (ưu tiên record từ entry mới nhất)
                if (!studentRecordsMap.containsKey(studentId)) {
                    studentRecordsMap.put(studentId, toRecordResponse(record));
                }
            }
        }

        // 8. Convert map thành list và sắp xếp theo tên học viên
        List<GradeRecordResponse> gradeRecords = new ArrayList<>(studentRecordsMap.values());
        gradeRecords.sort((a, b) -> {
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
        return response;
    }
}

