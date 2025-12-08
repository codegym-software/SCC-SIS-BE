package com.example.sis.services.impl;

import com.example.sis.dtos.lessons.LessonProgressUpdateRequest;
import com.example.sis.dtos.lessons.LessonProgressResponse;
import com.example.sis.enums.ProgressStatus;
import com.example.sis.models.LessonProgress;
import com.example.sis.repositories.LessonProgressRepository;
import com.example.sis.services.LessonProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LessonProgressServiceImpl implements LessonProgressService {
    
    private final LessonProgressRepository progressRepository;
    
    public LessonProgressServiceImpl(LessonProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }
    
    @Override
    @Transactional
    public LessonProgressResponse updateVideoProgress(Integer studentId, LessonProgressUpdateRequest request) {
        // Find existing or create new
        LessonProgress progress = progressRepository
            .findByStudentIdAndLessonId(studentId, request.getLessonId())
            .orElse(new LessonProgress());
        
        boolean isNew = progress.getProgressId() == null;
        
        // Set basic fields
        progress.setStudentId(studentId);
        progress.setLessonId(request.getLessonId());
        progress.setLastWatchedPosition(request.getCurrentPosition());
        progress.setLastAccessedAt(LocalDateTime.now());
        
        // Set first accessed time if new
        if (isNew && progress.getFirstAccessedAt() == null) {
            progress.setFirstAccessedAt(LocalDateTime.now());
        }
        
        // Update time spent (cumulative)
        if (request.getTimeSpent() != null) {
            int currentTimeSpent = progress.getTimeSpentSeconds() != null ? progress.getTimeSpentSeconds() : 0;
            progress.setTimeSpentSeconds(currentTimeSpent + request.getTimeSpent());
        }
        
        // Calculate progress percentage
        int percentage = 0;
        if (request.getDuration() != null && request.getDuration() > 0) {
            percentage = Math.min(100, 
                (int) ((request.getCurrentPosition() * 100.0) / request.getDuration()));
        }
        
        // Update status - but never downgrade from COMPLETED
        if (progress.getStatus() != ProgressStatus.COMPLETED) {
            // Only update if not already completed
            progress.setProgressPercentage(percentage);
            
            if (percentage >= 90 || (request.getCurrentPosition() >= request.getDuration() - 5)) {
                // Mark as completed if watched >= 90% or reached the end
                progress.setStatus(ProgressStatus.COMPLETED);
                progress.setProgressPercentage(100); // Set to 100% when completed
                if (progress.getCompletedAt() == null) {
                    progress.setCompletedAt(LocalDateTime.now());
                }
            } else if (percentage > 0) {
                // In progress if started watching
                progress.setStatus(ProgressStatus.IN_PROGRESS);
            }
        }
        // If already COMPLETED, keep it at 100% and don't change status
        
        progress = progressRepository.save(progress);
        
        return mapToResponse(progress);
    }
    
    @Override
    @Transactional(readOnly = true)
    public LessonProgressResponse getProgress(Integer studentId, Integer lessonId) {
        return progressRepository
            .findByStudentIdAndLessonId(studentId, lessonId)
            .map(this::mapToResponse)
            .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, LessonProgressResponse> getProgressForLessons(Integer studentId, List<Integer> lessonIds) {
        List<LessonProgress> progressList = progressRepository
            .findByStudentIdAndLessonIdIn(studentId, lessonIds);
        
        Map<Integer, LessonProgressResponse> result = new HashMap<>();
        for (LessonProgress progress : progressList) {
            result.put(progress.getLessonId(), mapToResponse(progress));
        }
        
        return result;
    }
    
    private LessonProgressResponse mapToResponse(LessonProgress progress) {
        LessonProgressResponse response = new LessonProgressResponse();
        response.setProgressId(progress.getProgressId());
        response.setStudentId(progress.getStudentId());
        response.setLessonId(progress.getLessonId());
        response.setStatus(progress.getStatus());
        response.setProgressPercentage(progress.getProgressPercentage());
        response.setTimeSpentSeconds(progress.getTimeSpentSeconds());
        response.setLastWatchedPosition(progress.getLastWatchedPosition());
        response.setCompletedAt(progress.getCompletedAt());
        response.setLastAccessedAt(progress.getLastAccessedAt());
        return response;
    }
}
