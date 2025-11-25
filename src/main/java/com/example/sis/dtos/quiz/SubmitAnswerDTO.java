package com.example.sis.dtos.quiz;

public class SubmitAnswerDTO {
    
    private Integer questionId;
    private Integer selectedOptionId;
    
    public SubmitAnswerDTO() {}

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Integer selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }
}
