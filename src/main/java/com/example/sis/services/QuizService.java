package com.example.sis.services;

import com.example.sis.dtos.quiz.*;
import com.example.sis.models.Quiz;
import com.example.sis.models.QuizOption;
import com.example.sis.models.QuizQuestion;
import com.example.sis.repositories.QuizOptionRepository;
import com.example.sis.repositories.QuizQuestionRepository;
import com.example.sis.repositories.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);
    
    @Autowired
    private QuizRepository quizRepository;
    
    @Autowired
    private QuizQuestionRepository questionRepository;
    
    @Autowired
    private QuizOptionRepository optionRepository;
    
    @Autowired
    private WordImportService wordImportService;
    
    /**
     * Tạo Quiz mới
     */
    @Transactional
    public Quiz createQuiz(CreateQuizDTO dto) {
        logger.info("📝 Creating new quiz: {}", dto.getQuizTitle());
        
        Quiz quiz = new Quiz();
        quiz.setLessonId(dto.getLessonId());
        quiz.setQuizTitle(dto.getQuizTitle());
        quiz.setQuizType(dto.getQuizType());
        quiz.setTimeLimitMinutes(dto.getTimeLimitMinutes());
        quiz.setPassingScore(dto.getPassingScore() != null ? dto.getPassingScore() : 70);
        quiz.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 3);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setDeleted(false);
        
        Quiz savedQuiz = quizRepository.save(quiz);
        logger.info("✅ Created quiz ID: {}", savedQuiz.getQuizId());
        
        return savedQuiz;
    }
    
    /**
     * Import câu hỏi từ file Word
     */
    @Transactional
    public String importQuestionsFromWord(Integer quizId, MultipartFile file) throws IOException {
        logger.info("📥 Importing questions for quiz ID: {}", quizId);
        
        // Kiểm tra quiz tồn tại
        Quiz quiz = quizRepository.findByQuizIdAndDeletedFalse(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found with ID: " + quizId));
        
        // Parse file Word
        List<QuestionImportDTO> questions = wordImportService.importQuestionsFromWord(file);
        
        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found in the file");
        }
        
        // Get current max question order
        int currentCount = questionRepository.countByQuizId(quizId);
        
        // Lưu câu hỏi vào database
        int savedCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuestionImportDTO qDto = questions.get(i);
            
            // Tạo question
            QuizQuestion question = new QuizQuestion();
            question.setQuizId(quizId);
            question.setQuestionText(qDto.getQuestionText());
            question.setQuestionOrder(currentCount + i + 1);
            question.setPoints(qDto.getPoints() != null ? qDto.getPoints() : 10);
            
            QuizQuestion savedQuestion = questionRepository.save(question);
            
            // Tạo options
            List<OptionImportDTO> optionDTOs = qDto.getOptions();
            for (int j = 0; j < optionDTOs.size(); j++) {
                OptionImportDTO oDto = optionDTOs.get(j);
                
                QuizOption option = new QuizOption();
                option.setQuestionId(savedQuestion.getQuestionId());
                option.setOptionText(oDto.getText());
                option.setIsCorrect(oDto.getIsCorrect() != null ? oDto.getIsCorrect() : false);
                option.setOptionOrder(j + 1);
                
                optionRepository.save(option);
            }
            
            savedCount++;
        }
        
        logger.info("✅ Imported {} questions successfully", savedCount);
        return "Successfully imported " + savedCount + " questions";
    }
    
    /**
     * Lấy chi tiết quiz (cho admin - có đáp án)
     */
    public QuizDetailDTO getQuizDetail(Integer lessonId, boolean includeAnswers) {
        Quiz quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new RuntimeException("Quiz not found for lesson ID: " + lessonId));
        
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByQuestionOrder(quiz.getQuizId());
        
        // Get all question IDs
        List<Integer> questionIds = new ArrayList<>();
        for (QuizQuestion q : questions) {
            questionIds.add(q.getQuestionId());
        }
        
        // Get all options
        List<QuizOption> allOptions = questionIds.isEmpty() ? 
                new ArrayList<>() : 
                optionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds);
        
        // Group options by question ID
        List<QuestionDetailDTO> questionDTOs = new ArrayList<>();
        int totalPoints = 0;
        
        for (QuizQuestion question : questions) {
            QuestionDetailDTO qDto = new QuestionDetailDTO();
            qDto.setQuestionId(question.getQuestionId());
            qDto.setQuestionText(question.getQuestionText());
            qDto.setQuestionOrder(question.getQuestionOrder());
            qDto.setPoints(question.getPoints());
            totalPoints += question.getPoints();
            
            // Get options for this question
            List<OptionDetailDTO> optionDTOs = new ArrayList<>();
            for (QuizOption option : allOptions) {
                if (option.getQuestionId().equals(question.getQuestionId())) {
                    OptionDetailDTO oDto = new OptionDetailDTO();
                    oDto.setOptionId(option.getOptionId());
                    oDto.setOptionText(option.getOptionText());
                    oDto.setOptionOrder(option.getOptionOrder());
                    
                    // Chỉ hiển thị đáp án nếu includeAnswers = true (admin)
                    if (includeAnswers) {
                        oDto.setIsCorrect(option.getIsCorrect());
                    }
                    
                    optionDTOs.add(oDto);
                }
            }
            
            qDto.setOptions(optionDTOs);
            questionDTOs.add(qDto);
        }
        
        // Build response
        QuizDetailDTO dto = new QuizDetailDTO();
        dto.setQuizId(quiz.getQuizId());
        dto.setLessonId(quiz.getLessonId());
        dto.setQuizTitle(quiz.getQuizTitle());
        dto.setQuizType(quiz.getQuizType().name());
        dto.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        dto.setPassingScore(quiz.getPassingScore());
        dto.setMaxAttempts(quiz.getMaxAttempts());
        dto.setTotalQuestions(questions.size());
        dto.setTotalPoints(totalPoints);
        dto.setQuestions(questionDTOs);
        
        return dto;
    }
}
