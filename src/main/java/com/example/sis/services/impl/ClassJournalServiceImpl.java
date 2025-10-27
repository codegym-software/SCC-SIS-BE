package com.example.sis.services.impl;

import com.example.sis.dtos.journal.CreateJournalRequest;
import com.example.sis.dtos.journal.JournalResponse;
import com.example.sis.dtos.journal.UpdateJournalRequest;
import com.example.sis.enums.JournalType;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.ResourceNotFoundException;
import com.example.sis.models.ClassEntity;
import com.example.sis.models.ClassJournal;
import com.example.sis.models.User;
import com.example.sis.repositories.ClassJournalRepository;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.ClassJournalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassJournalServiceImpl implements ClassJournalService {

    private static final Logger log = LoggerFactory.getLogger(ClassJournalServiceImpl.class);

    private final ClassJournalRepository classJournalRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public ClassJournalServiceImpl(ClassJournalRepository classJournalRepository,
                                   ClassRepository classRepository,
                                   UserRepository userRepository) {
        this.classJournalRepository = classJournalRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    @Override
    public JournalResponse createJournal(CreateJournalRequest request, Integer createdByUserId) {
        log.info("Creating journal for class {} by user {}", request.getClassId(), createdByUserId);

        // Kiểm tra lớp học tồn tại
        ClassEntity classEntity = classRepository.findByIdAndNotDeleted(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        // Kiểm tra giảng viên tồn tại
        User teacher = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));

        // Tạo entity mới
        ClassJournal journal = new ClassJournal();
        journal.setClassEntity(classEntity);
        journal.setTeacher(teacher);
        journal.setTitle(request.getTitle());
        journal.setContent(request.getContent());
        journal.setJournalDate(request.getJournalDate());
        journal.setJournalTime(request.getJournalTime());
        
        // Parse journal type (mặc định là OTHER nếu không có hoặc invalid)
        try {
            if (request.getJournalType() != null && !request.getJournalType().isBlank()) {
                journal.setJournalType(JournalType.valueOf(request.getJournalType().toUpperCase()));
            } else {
                journal.setJournalType(JournalType.OTHER);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid journal type: {}, using OTHER", request.getJournalType());
            journal.setJournalType(JournalType.OTHER);
        }

        journal.setCreatedBy(teacher);
        journal.setUpdatedBy(teacher);

        // Database trigger sẽ kiểm tra xem teacher có được phân vào lớp này không
        // Nếu không hợp lệ, trigger sẽ throw exception
        ClassJournal savedJournal = classJournalRepository.save(journal);

        log.info("Journal created successfully with ID: {}", savedJournal.getJournalId());
        return mapToResponse(savedJournal);
    }

    @Override
    public JournalResponse updateJournal(Integer journalId, UpdateJournalRequest request, Integer updatedByUserId, boolean isSuperAdmin) {
        log.info("Updating journal {} by user {} (isSuperAdmin: {})", journalId, updatedByUserId, isSuperAdmin);

        // Tìm nhật ký
        ClassJournal journal = classJournalRepository.findByIdNotDeleted(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký"));

        // Kiểm tra quyền: chỉ chủ sở hữu hoặc SUPER_ADMIN mới được cập nhật
        if (!isSuperAdmin && !journal.getTeacher().getUserId().equals(updatedByUserId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa nhật ký này");
        }

        // Tìm User để set updatedBy
        User updater = userRepository.findById(updatedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Cập nhật các trường
        if (request.getTitle() != null) {
            journal.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            journal.setContent(request.getContent());
        }
        if (request.getJournalDate() != null) {
            journal.setJournalDate(request.getJournalDate());
        }
        if (request.getJournalTime() != null) {
            journal.setJournalTime(request.getJournalTime());
        }
        if (request.getJournalType() != null && !request.getJournalType().isBlank()) {
            try {
                journal.setJournalType(JournalType.valueOf(request.getJournalType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid journal type: {}, keeping existing type", request.getJournalType());
            }
        }

        journal.setUpdatedBy(updater);

        ClassJournal updatedJournal = classJournalRepository.save(journal);
        log.info("Journal {} updated successfully", journalId);

        return mapToResponse(updatedJournal);
    }

    @Override
    public void softDeleteJournal(Integer journalId, Integer deletedByUserId, boolean isSuperAdmin) {
        log.info("Soft deleting journal {} by user {} (isSuperAdmin: {})", journalId, deletedByUserId, isSuperAdmin);

        // Tìm nhật ký
        ClassJournal journal = classJournalRepository.findByIdNotDeleted(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký"));

        // Kiểm tra quyền: chỉ chủ sở hữu hoặc SUPER_ADMIN mới được xóa
        if (!isSuperAdmin && !journal.getTeacher().getUserId().equals(deletedByUserId)) {
            throw new BadRequestException("Bạn không có quyền xóa nhật ký này");
        }

        // Tìm User để set updatedBy
        User deleter = userRepository.findById(deletedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Soft delete
        journal.setDeletedAt(LocalDateTime.now());
        journal.setUpdatedBy(deleter);

        classJournalRepository.save(journal);
        log.info("Journal {} soft deleted successfully", journalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalResponse> getJournalsByClass(Integer classId) {
        log.info("Getting journals for class {}", classId);

        // Kiểm tra lớp học tồn tại
        classRepository.findByIdAndNotDeleted(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        List<ClassJournal> journals = classJournalRepository.findAllByClassIdNotDeleted(classId);
        return journals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalResponse> getJournalsByTeacher(Integer teacherId) {
        log.info("Getting journals for teacher {}", teacherId);

        // Kiểm tra giảng viên tồn tại
        userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));

        List<ClassJournal> journals = classJournalRepository.findAllByTeacherIdNotDeleted(teacherId);
        return journals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JournalResponse getJournalById(Integer journalId) {
        log.info("Getting journal {}", journalId);

        ClassJournal journal = classJournalRepository.findByIdNotDeleted(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký"));

        return mapToResponse(journal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalResponse> getJournalsByClassAndType(Integer classId, String journalType) {
        log.info("Getting journals for class {} with type {}", classId, journalType);

        // Kiểm tra lớp học tồn tại
        classRepository.findByIdAndNotDeleted(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        // Validate journal type
        try {
            JournalType.valueOf(journalType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại nhật ký không hợp lệ: " + journalType);
        }

        List<ClassJournal> journals = classJournalRepository.findByClassIdAndType(classId, journalType.toUpperCase());
        return journals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map ClassJournal entity sang JournalResponse DTO
     */
    private JournalResponse mapToResponse(ClassJournal journal) {
        JournalResponse response = new JournalResponse();
        response.setJournalId(journal.getJournalId());
        response.setClassId(journal.getClassEntity().getClassId());
        response.setClassName(journal.getClassEntity().getName()); // ClassEntity uses getName()
        response.setTeacherId(journal.getTeacher().getUserId());
        response.setTeacherName(journal.getTeacher().getFullName());
        response.setTitle(journal.getTitle());
        response.setContent(journal.getContent());
        response.setJournalDate(journal.getJournalDate());
        response.setJournalTime(journal.getJournalTime());
        response.setJournalType(journal.getJournalType().name());
        response.setCreatedAt(journal.getCreatedAt());
        response.setUpdatedAt(journal.getUpdatedAt());
        return response;
    }
}
