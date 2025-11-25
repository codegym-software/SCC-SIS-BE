package com.example.sis.dtos.quiz;

public class OptionDetailDTO {
    
    private Integer optionId;
    private String optionText;
    private Integer optionOrder;
    private Boolean isCorrect; // Chỉ hiển thị cho admin hoặc sau khi submit
    
    public OptionDetailDTO() {}

    public Integer getOptionId() {
        return optionId;
    }

    public void setOptionId(Integer optionId) {
        this.optionId = optionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public Integer getOptionOrder() {
        return optionOrder;
    }

    public void setOptionOrder(Integer optionOrder) {
        this.optionOrder = optionOrder;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
