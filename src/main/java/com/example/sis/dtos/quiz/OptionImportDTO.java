package com.example.sis.dtos.quiz;

public class OptionImportDTO {
    
    private String text;
    private Boolean isCorrect;
    
    public OptionImportDTO() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
