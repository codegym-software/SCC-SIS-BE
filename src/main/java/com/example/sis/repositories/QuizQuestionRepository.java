package com.example.sis.repositories;

import com.example.sis.models.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {
    
    List<QuizQuestion> findByQuizIdOrderByQuestionOrder(Integer quizId);
    
    int countByQuizId(Integer quizId);
}
