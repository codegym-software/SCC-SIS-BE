package com.example.sis.repositories;

import com.example.sis.models.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
    
    Optional<LessonProgress> findByStudentIdAndLessonId(Integer studentId, Integer lessonId);
    
    List<LessonProgress> findByStudentIdAndLessonIdIn(Integer studentId, List<Integer> lessonIds);
}
