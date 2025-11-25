package com.example.sis.dtos.quiz;

public class StartAttemptRequestDTO {
    
    private Integer quizId;
    
    public StartAttemptRequestDTO() {}

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }
}
