package com.example.sis.repositories;

import com.example.sis.models.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    
    List<QuizAnswer> findByAttemptIdOrderByAnsweredAt(Integer attemptId);
    
    Optional<QuizAnswer> findByAttemptIdAndQuestionId(Integer attemptId, Integer questionId);
    
    int countByAttemptIdAndIsCorrectTrue(Integer attemptId);
}
