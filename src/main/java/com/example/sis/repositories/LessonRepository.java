package com.example.sis.repositories;

import com.example.sis.models.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    
    List<Lesson> findByModuleIdAndDeletedFalseOrderByLessonOrder(Integer moduleId);
    
    List<Lesson> findByModuleIdAndDeletedFalse(Integer moduleId);
}
