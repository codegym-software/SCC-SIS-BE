package com.example.sis.dtos.quiz;

import java.util.List;

public class QuestionDetailDTO {
    
    private Integer questionId;
    private String questionText;
    private Integer questionOrder;
    private Integer points;
    private List<OptionDetailDTO> options;
    
    public QuestionDetailDTO() {}

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<OptionDetailDTO> getOptions() {
        return options;
    }

    public void setOptions(List<OptionDetailDTO> options) {
        this.options = options;
    }
}
