package com.example.sis.repositories;

import com.example.sis.models.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    
    Optional<Quiz> findByLessonIdAndDeletedFalse(Integer lessonId);
    
    Optional<Quiz> findByQuizIdAndDeletedFalse(Integer quizId);
}
