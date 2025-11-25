package com.example.sis.repositories;

import com.example.sis.models.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Integer> {
    
    List<QuizOption> findByQuestionIdOrderByOptionOrder(Integer questionId);
    
    Optional<QuizOption> findByQuestionIdAndIsCorrectTrue(Integer questionId);
    
    List<QuizOption> findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(List<Integer> questionIds);
}
