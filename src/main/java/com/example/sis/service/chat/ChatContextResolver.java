package com.example.sis.service.chat;

import com.example.sis.dto.chat.SafeChatContext;
// import com.example.sis.entity.User;
// import com.example.sis.repository.EnrollmentRepository;
// import com.example.sis.repository.LessonProgressRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
// @RequiredArgsConstructor
// @Slf4j
public class ChatContextResolver {
    
    // TODO: Restore when User entity exists
    // private final EnrollmentRepository enrollmentRepository;
    // private final LessonProgressRepository lessonProgressRepository;
    
    /**
     * STUB: Return empty context until User entity is available
     */
    public SafeChatContext resolveContext(Object user, Integer explicitClassId, Integer explicitModuleId, Integer explicitLessonId) {
        return SafeChatContext.builder().language("vi").build();
    }
    
    /*
    // Original implementation - restore when User entity exists
    public SafeChatContext resolveContext(User user, Integer explicitClassId, Integer explicitModuleId, Integer explicitLessonId) {
        SafeChatContext.SafeChatContextBuilder builder = SafeChatContext.builder()
            .userName(user.getFullName())
            .userRole(user.getRole().name())
            .language("vi"); // Default language
        
        // 1. Try explicit context from request
        if (explicitLessonId != null) {
            return resolveFromLesson(builder, user.getUserId(), explicitLessonId);
        }
        
        if (explicitModuleId != null) {
            return resolveFromModule(builder, explicitModuleId);
        }
        
        if (explicitClassId != null) {
            return resolveFromClass(builder, explicitClassId);
        }
        
        // 2. Try to find current lesson from recent progress
        var recentProgress = lessonProgressRepository.findTopByUser_UserIdOrderByUpdatedAtDesc(user.getUserId());
        if (recentProgress.isPresent()) {
            var lesson = recentProgress.get().getLesson();
            return builder
                .lessonId(lesson.getLessonId())
                .lessonTitle(lesson.getTitle())
                .moduleId(lesson.getModule().getModuleId())
                .moduleName(lesson.getModule().getTitle())
                .classId(lesson.getModule().getCourse().getCourseId())
                .className(lesson.getModule().getCourse().getCourseTitle())
                .build();
        }
        
        // 3. Try to find active enrollment
        var activeEnrollment = enrollmentRepository.findFirstByUser_UserIdOrderByEnrollmentIdDesc(user.getUserId());
        if (activeEnrollment.isPresent()) {
            var clazz = activeEnrollment.get().getClazz();
            return builder
                .classId(clazz.getClassId())
                .className(clazz.getClassName())
                .build();
        }
        
        // 4. No context found - general chat only
        log.info("No academic context found for user: {}", user.getUserId());
        return builder.build();
    }
    
    private SafeChatContext resolveFromLesson(SafeChatContext.SafeChatContextBuilder builder, Integer userId, Integer lessonId) {
        var progress = lessonProgressRepository.findByUser_UserIdAndLesson_LessonId(userId, lessonId);
        if (progress.isPresent()) {
            var lesson = progress.get().getLesson();
            return builder
                .lessonId(lesson.getLessonId())
                .lessonTitle(lesson.getTitle())
                .moduleId(lesson.getModule().getModuleId())
                .moduleName(lesson.getModule().getTitle())
                .classId(lesson.getModule().getCourse().getCourseId())
                .className(lesson.getModule().getCourse().getCourseTitle())
                .build();
        }
        return builder.build();
    }
    
    private SafeChatContext resolveFromModule(SafeChatContext.SafeChatContextBuilder builder, Integer moduleId) {
        // TODO: Add ModuleRepository to fetch module details
        return builder.moduleId(moduleId).build();
    }
    
    private SafeChatContext resolveFromClass(SafeChatContext.SafeChatContextBuilder builder, Integer classId) {
        // TODO: Add ClassRepository to fetch class details
        return builder.classId(classId).build();
    }
    */
}
