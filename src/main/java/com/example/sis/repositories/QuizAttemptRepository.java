package com.example.sis.repositories;

import com.example.sis.enums.QuizAttemptStatus;
import com.example.sis.models.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {
    
    List<QuizAttempt> findByStudentIdAndQuizIdOrderByStartedAtDesc(Integer studentId, Integer quizId);
    
    int countByStudentIdAndQuizIdAndStatus(Integer studentId, Integer quizId, QuizAttemptStatus status);
    
    Optional<QuizAttempt> findByAttemptIdAndStudentId(Integer attemptId, Integer studentId);
    
    List<QuizAttempt> findByQuizIdAndStatus(Integer quizId, QuizAttemptStatus status);
}
