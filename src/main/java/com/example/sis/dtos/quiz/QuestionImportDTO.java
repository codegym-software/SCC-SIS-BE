package com.example.sis.dtos.quiz;

import java.util.List;

public class QuestionImportDTO {
    
    private String questionText;
    private Integer points;
    private List<OptionImportDTO> options;
    
    public QuestionImportDTO() {}

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<OptionImportDTO> getOptions() {
        return options;
    }

    public void setOptions(List<OptionImportDTO> options) {
        this.options = options;
    }
}
