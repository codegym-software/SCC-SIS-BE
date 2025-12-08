package com.example.sis.services;

import com.example.sis.dtos.lesson.*;
import com.example.sis.enums.ContentType;
import com.example.sis.enums.ProgressStatus;
import com.example.sis.models.Lesson;
import com.example.sis.models.LessonProgress;
import com.example.sis.models.Module;
import com.example.sis.repositories.LessonProgressRepository;
import com.example.sis.repositories.LessonRepository;
import com.example.sis.repositories.ModuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LessonService {
    
    @Autowired
    private LessonRepository lessonRepository;
    
    @Autowired
    private ModuleRepository moduleRepository;
    
    @Autowired
    private LessonProgressRepository lessonProgressRepository;
    
    @Autowired
    private VimeoService vimeoService;
    
    /**
     * API 1: Get single lesson by ID with student progress
     * @param studentId - null nếu user không phải student
     */
    public LessonResponseDTO getLessonById(Integer lessonId, Integer studentId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        
        if (lesson.getDeleted()) {
            throw new RuntimeException("Lesson has been deleted");
        }
        
        // Get module to retrieve semester info
        Module module = moduleRepository.findById(lesson.getModuleId()).orElse(null);
        Integer moduleSemester = module != null ? module.getSemester() : null;
        
        LessonResponseDTO dto = new LessonResponseDTO();
        
        // Lesson information
        dto.setLessonId(lesson.getLessonId());
        dto.setModuleId(lesson.getModuleId());
        dto.setLessonTitle(lesson.getLessonTitle());
        dto.setLessonType(lesson.getLessonType());
        dto.setLessonOrder(lesson.getLessonOrder());
        dto.setContentUrl(lesson.getContentUrl());
        dto.setContentType(lesson.getContentType());
        dto.setDurationMinutes(lesson.getDurationMinutes());
        dto.setDescription(lesson.getDescription());
        dto.setIsMandatory(lesson.getIsMandatory());
        dto.setPassingScore(lesson.getPassingScore());
        dto.setModuleSemester(moduleSemester);
        
        // Progress information (nếu là student)
        if (studentId != null) {
            LessonProgress progress = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                    .orElse(null);
            
            if (progress != null) {
                dto.setStatus(progress.getStatus());
                dto.setProgressPercentage(progress.getProgressPercentage());
                dto.setTimeSpentSeconds(progress.getTimeSpentSeconds());
                dto.setLastWatchedPosition(progress.getLastWatchedPosition());
                dto.setCompletedAt(progress.getCompletedAt());
                dto.setLastAccessedAt(progress.getLastAccessedAt());
            } else {
                dto.setStatus(ProgressStatus.NOT_STARTED);
                dto.setProgressPercentage(0);
                dto.setTimeSpentSeconds(0);
                dto.setLastWatchedPosition(0);
            }
        }
        
        return dto;
    }
    
    /**
     * API 1.2: Get lessons by module with student progress
     * @param studentId - null nếu user không phải student (admin/manager chỉ xem lessons)
     */
    public List<LessonResponseDTO> getLessonsByModule(Integer moduleId, Integer studentId) {
        // Get all lessons for the module
        List<Lesson> lessons = lessonRepository.findByModuleIdAndDeletedFalseOrderByLessonOrder(moduleId);
        
        // Get module to retrieve semester info and module name
        Module module = moduleRepository.findById(moduleId).orElse(null);
        Integer moduleSemester = module != null ? module.getSemester() : null;
        String moduleName = module != null ? module.getName() : null;
        
        // Get lesson IDs
        List<Integer> lessonIds = new ArrayList<>();
        for (Lesson lesson : lessons) {
            lessonIds.add(lesson.getLessonId());
        }
        
        // Get progress for all lessons (only if studentId is provided)
        List<LessonProgress> progressList = new ArrayList<>();
        if (studentId != null && !lessonIds.isEmpty()) {
            progressList = lessonProgressRepository.findByStudentIdAndLessonIdIn(studentId, lessonIds);
        }
        
        // Map progress by lessonId
        Map<Integer, LessonProgress> progressMap = new HashMap<>();
        for (LessonProgress progress : progressList) {
            progressMap.put(progress.getLessonId(), progress);
        }
        
        // Build response
        List<LessonResponseDTO> response = new ArrayList<>();
        for (Lesson lesson : lessons) {
            LessonResponseDTO dto = new LessonResponseDTO();
            
            // Lesson information
            dto.setLessonId(lesson.getLessonId());
            dto.setModuleId(lesson.getModuleId());
            dto.setLessonTitle(lesson.getLessonTitle());
            dto.setLessonType(lesson.getLessonType());
            dto.setLessonOrder(lesson.getLessonOrder());
            dto.setContentUrl(lesson.getContentUrl());
            dto.setContentType(lesson.getContentType());
            dto.setDurationMinutes(lesson.getDurationMinutes());
            dto.setDescription(lesson.getDescription());
            dto.setIsMandatory(lesson.getIsMandatory());
            dto.setPassingScore(lesson.getPassingScore());
            dto.setModuleSemester(moduleSemester);
            dto.setModuleName(moduleName);
            
            // Progress information
            LessonProgress progress = progressMap.get(lesson.getLessonId());
            if (progress != null) {
                dto.setStatus(progress.getStatus());
                dto.setProgressPercentage(progress.getProgressPercentage());
                dto.setTimeSpentSeconds(progress.getTimeSpentSeconds());
                dto.setLastWatchedPosition(progress.getLastWatchedPosition());
                dto.setCompletedAt(progress.getCompletedAt());
                dto.setLastAccessedAt(progress.getLastAccessedAt());
            } else {
                dto.setStatus(ProgressStatus.NOT_STARTED);
                dto.setProgressPercentage(0);
                dto.setTimeSpentSeconds(0);
                dto.setLastWatchedPosition(0);
            }
            
            response.add(dto);
        }
        
        return response;
    }
    
    /**
     * API NEW: Get all lessons by class (includes lessons from current and next semester)
     * @param classId - The class ID
     * @param studentId - null nếu user không phải student
     */
    public List<LessonResponseDTO> getLessonsByClass(Integer classId, Integer studentId) {
        // Get all modules for the program that this class belongs to
        // For now, we'll get all modules and their lessons, then filter by semester if needed
        // This is a simplified implementation - you may need to adjust based on your class-program relationship
        
        List<LessonResponseDTO> allLessons = new ArrayList<>();
        
        // Get all modules (you may need to filter by program based on classId)
        // For now, getting all lessons from all modules
        // TODO: Implement proper filtering based on class -> program -> modules relationship
        
        List<Module> modules = moduleRepository.findAll(); // Simplified - should filter by class's program
        
        for (Module module : modules) {
            if (module.getDeletedAt() != null) {
                continue;
            }
            
            List<Lesson> lessons = lessonRepository.findByModuleIdAndDeletedFalseOrderByLessonOrder(module.getModuleId());
            Integer moduleSemester = module.getSemester();
            String moduleName = module.getName();
            
            for (Lesson lesson : lessons) {
                LessonResponseDTO dto = new LessonResponseDTO();
                
                // Lesson information
                dto.setLessonId(lesson.getLessonId());
                dto.setModuleId(lesson.getModuleId());
                dto.setLessonTitle(lesson.getLessonTitle());
                dto.setLessonType(lesson.getLessonType());
                dto.setLessonOrder(lesson.getLessonOrder());
                dto.setContentUrl(lesson.getContentUrl());
                dto.setContentType(lesson.getContentType());
                dto.setDurationMinutes(lesson.getDurationMinutes());
                dto.setDescription(lesson.getDescription());
                dto.setIsMandatory(lesson.getIsMandatory());
                dto.setPassingScore(lesson.getPassingScore());
                dto.setModuleSemester(moduleSemester);
                dto.setModuleName(moduleName);
                
                // Progress information (if student)
                if (studentId != null) {
                    LessonProgress progress = lessonProgressRepository
                            .findByStudentIdAndLessonId(studentId, lesson.getLessonId())
                            .orElse(null);
                    
                    if (progress != null) {
                        dto.setStatus(progress.getStatus());
                        dto.setProgressPercentage(progress.getProgressPercentage());
                        dto.setTimeSpentSeconds(progress.getTimeSpentSeconds());
                        dto.setLastWatchedPosition(progress.getLastWatchedPosition());
                        dto.setCompletedAt(progress.getCompletedAt());
                        dto.setLastAccessedAt(progress.getLastAccessedAt());
                    } else {
                        dto.setStatus(ProgressStatus.NOT_STARTED);
                        dto.setProgressPercentage(0);
                        dto.setTimeSpentSeconds(0);
                        dto.setLastWatchedPosition(0);
                    }
                }
                
                allLessons.add(dto);
            }
        }
        
        return allLessons;
    }
    
    /**
     * API 2: Update video progress
     */
    @Transactional
    public LessonProgress updateProgress(Integer lessonId, Integer studentId, VideoProgressRequestDTO request) {
        // Find existing progress or create new
        LessonProgress progress = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(new LessonProgress());
        
        if (progress.getProgressId() == null) {
            // New progress
            progress.setStudentId(studentId);
            progress.setLessonId(lessonId);
            progress.setFirstAccessedAt(LocalDateTime.now());
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }
        
        // Update progress data
        if (request.getProgressPercentage() != null) {
            progress.setProgressPercentage(request.getProgressPercentage());
            
            // Auto-complete if 100%
            if (request.getProgressPercentage() >= 100 && progress.getStatus() != ProgressStatus.COMPLETED) {
                progress.setStatus(ProgressStatus.COMPLETED);
                progress.setCompletedAt(LocalDateTime.now());
            } else if (request.getProgressPercentage() > 0 && progress.getStatus() == ProgressStatus.NOT_STARTED) {
                progress.setStatus(ProgressStatus.IN_PROGRESS);
            }
        }
        
        if (request.getLastWatchedPosition() != null) {
            progress.setLastWatchedPosition(request.getLastWatchedPosition());
        }
        
        if (request.getTimeSpentSeconds() != null) {
            progress.setTimeSpentSeconds(request.getTimeSpentSeconds());
        }
        
        progress.setLastAccessedAt(LocalDateTime.now());
        
        return lessonProgressRepository.save(progress);
    }
    
    /**
     * API 3: Get module progress
     */
    public ModuleProgressResponseDTO getModuleProgress(Integer moduleId, Integer studentId) {
        // Get all lessons
        List<Lesson> lessons = lessonRepository.findByModuleIdAndDeletedFalse(moduleId);
        
        // Get lesson IDs
        List<Integer> lessonIds = new ArrayList<>();
        for (Lesson lesson : lessons) {
            lessonIds.add(lesson.getLessonId());
        }
        
        // Get progress
        List<LessonProgress> progressList = lessonProgressRepository.findByStudentIdAndLessonIdIn(studentId, lessonIds);
        
        // Calculate statistics
        int totalLessons = lessons.size();
        int completedLessons = 0;
        int totalTimeSpent = 0;
        
        for (LessonProgress progress : progressList) {
            if (progress.getStatus() == ProgressStatus.COMPLETED) {
                completedLessons++;
            }
            if (progress.getTimeSpentSeconds() != null) {
                totalTimeSpent += progress.getTimeSpentSeconds();
            }
        }
        
        int progressPercentage = totalLessons > 0 ? (completedLessons * 100) / totalLessons : 0;
        
        // Build response
        ModuleProgressResponseDTO response = new ModuleProgressResponseDTO();
        response.setModuleId(moduleId);
        response.setTotalLessons(totalLessons);
        response.setCompletedLessons(completedLessons);
        response.setProgressPercentage(progressPercentage);
        response.setTotalTimeSpentSeconds(totalTimeSpent);
        
        return response;
    }
    
    /**
     * API 4: Create lesson (Admin)
     */
    @Transactional
    public Lesson createLesson(CreateLessonRequestDTO request) {
        try {
            System.out.println("=== CREATE LESSON SERVICE ===");
            System.out.println("Validating module ID: " + request.getModuleId());
            
            // Validate module exists
            Module module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new RuntimeException("Module not found with id: " + request.getModuleId()));
            
            System.out.println("Module found: " + module.getName());
            
            Lesson lesson = new Lesson();
            
            lesson.setModuleId(request.getModuleId());
            lesson.setLessonTitle(request.getLessonTitle());
            lesson.setLessonType(request.getLessonType());
            lesson.setLessonOrder(request.getLessonOrder());
            lesson.setContentUrl(request.getContentUrl());
            lesson.setContentType(request.getContentType());
            lesson.setDescription(request.getDescription());
            lesson.setIsMandatory(request.getIsMandatory() != null ? request.getIsMandatory() : true);
            lesson.setPassingScore(request.getPassingScore() != null ? request.getPassingScore() : 70);
            lesson.setDeleted(false);
            
            // 🎯 TỰ ĐỘNG LẤY DURATION TỪ VIMEO
            if (request.getContentType() == ContentType.VIMEO && request.getContentUrl() != null) {
                try {
                    Integer duration = vimeoService.getVideoDuration(request.getContentUrl());
                    if (duration != null) {
                        lesson.setDurationMinutes(duration);
                        System.out.println("✅ Auto-fetched duration from Vimeo: " + duration + " minutes");
                    } else {
                        // Fallback to manual duration
                        lesson.setDurationMinutes(request.getDurationMinutes());
                        System.out.println("⚠️ Could not fetch duration from Vimeo, using manual: " + 
                                request.getDurationMinutes() + " minutes");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error fetching Vimeo duration: " + e.getMessage());
                    lesson.setDurationMinutes(request.getDurationMinutes());
                }
            } else {
                lesson.setDurationMinutes(request.getDurationMinutes());
            }
            
            System.out.println("Saving lesson to database...");
            Lesson savedLesson = lessonRepository.save(lesson);
            System.out.println("✅ Created lesson ID: " + savedLesson.getLessonId() + 
                    " with duration: " + savedLesson.getDurationMinutes() + " minutes");
            
            return savedLesson;
        } catch (Exception e) {
            System.err.println("=== ERROR IN CREATE LESSON SERVICE ===");
            e.printStackTrace();
            throw e;
        }
    }
}
