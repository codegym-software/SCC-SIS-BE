package com.example.sis.dtos.quiz;

import java.util.List;

public class BulkAnswersRequestDTO {
    private List<SubmitAnswerDTO> answers;

    public List<SubmitAnswerDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SubmitAnswerDTO> answers) {
        this.answers = answers;
    }
}
