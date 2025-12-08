package com.example.sis.services;

import com.example.sis.dtos.lessons.LessonProgressUpdateRequest;
import com.example.sis.dtos.lessons.LessonProgressResponse;

import java.util.List;
import java.util.Map;

public interface LessonProgressService {
    
    /**
     * Update lesson progress for video watching
     */
    LessonProgressResponse updateVideoProgress(Integer studentId, LessonProgressUpdateRequest request);
    
    /**
     * Get progress for a specific lesson
     */
    LessonProgressResponse getProgress(Integer studentId, Integer lessonId);
    
    /**
     * Get progress for multiple lessons (for module view)
     */
    Map<Integer, LessonProgressResponse> getProgressForLessons(Integer studentId, List<Integer> lessonIds);
}
