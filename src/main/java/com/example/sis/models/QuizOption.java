package com.example.sis.models;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_options")
public class QuizOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Integer optionId;
    
    @Column(name = "question_id", nullable = false)
    private Integer questionId;
    
    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;
    
    @Column(name = "is_correct")
    private Boolean isCorrect = false;
    
    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;

    // Getters and Setters
    public Integer getOptionId() { return optionId; }
    public void setOptionId(Integer optionId) { this.optionId = optionId; }

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public Integer getOptionOrder() { return optionOrder; }
    public void setOptionOrder(Integer optionOrder) { this.optionOrder = optionOrder; }
}
