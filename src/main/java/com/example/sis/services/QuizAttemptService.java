package com.example.sis.services;

import com.example.sis.dtos.quiz.*;
import com.example.sis.enums.ProgressStatus;
import com.example.sis.enums.QuizAttemptStatus;
import com.example.sis.models.*;
import com.example.sis.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuizAttemptService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuizAttemptService.class);
    
    @Autowired
    private QuizAttemptRepository attemptRepository;
    
    @Autowired
    private QuizAnswerRepository answerRepository;
    
    @Autowired
    private QuizRepository quizRepository;
    
    @Autowired
    private QuizQuestionRepository questionRepository;
    
    @Autowired
    private QuizOptionRepository optionRepository;
    
    @Autowired
    private LessonProgressRepository lessonProgressRepository;
    
    /**
     * Bắt đầu làm quiz
     */
    @Transactional
    public StartAttemptResponseDTO startAttempt(Integer studentId, Integer quizId) {
        logger.info("🎯 Student {} starting quiz {}", studentId, quizId);
        
        // Kiểm tra quiz tồn tại
        Quiz quiz = quizRepository.findByQuizIdAndDeletedFalse(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        // Kiểm tra số lần làm bài
        List<QuizAttempt> previousAttempts = attemptRepository.findByStudentIdAndQuizIdOrderByStartedAtDesc(studentId, quizId);
        int completedAttempts = 0;
        for (QuizAttempt attempt : previousAttempts) {
            if (attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
                completedAttempts++;
            }
        }
        
        if (completedAttempts >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Maximum attempts reached (" + quiz.getMaxAttempts() + ")");
        }
        
        // Get questions
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByQuestionOrder(quizId);
        
        if (questions.isEmpty()) {
            throw new RuntimeException("Quiz has no questions");
        }
        
        // Calculate total points
        int totalPoints = 0;
        for (QuizQuestion q : questions) {
            totalPoints += q.getPoints();
        }
        
        // Tạo attempt
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudentId(studentId);
        attempt.setQuizId(quizId);
        attempt.setTotalPoints(totalPoints);
        attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());
        
        QuizAttempt savedAttempt = attemptRepository.save(attempt);
        
        // Get options for questions
        List<Integer> questionIds = new ArrayList<>();
        for (QuizQuestion q : questions) {
            questionIds.add(q.getQuestionId());
        }
        
        List<QuizOption> allOptions = optionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds);
        
        // Build question DTOs (KHÔNG bao gồm đáp án đúng)
        List<QuestionDetailDTO> questionDTOs = new ArrayList<>();
        for (QuizQuestion question : questions) {
            QuestionDetailDTO qDto = new QuestionDetailDTO();
            qDto.setQuestionId(question.getQuestionId());
            qDto.setQuestionText(question.getQuestionText());
            qDto.setQuestionOrder(question.getQuestionOrder());
            qDto.setPoints(question.getPoints());
            
            List<OptionDetailDTO> optionDTOs = new ArrayList<>();
            for (QuizOption option : allOptions) {
                if (option.getQuestionId().equals(question.getQuestionId())) {
                    OptionDetailDTO oDto = new OptionDetailDTO();
                    oDto.setOptionId(option.getOptionId());
                    oDto.setOptionText(option.getOptionText());
                    oDto.setOptionOrder(option.getOptionOrder());
                    // KHÔNG set isCorrect - student không được biết đáp án
                    optionDTOs.add(oDto);
                }
            }
            
            qDto.setOptions(optionDTOs);
            questionDTOs.add(qDto);
        }
        
        // Build response
        StartAttemptResponseDTO response = new StartAttemptResponseDTO();
        response.setAttemptId(savedAttempt.getAttemptId());
        response.setQuizId(quiz.getQuizId());
        response.setQuizTitle(quiz.getQuizTitle());
        response.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        response.setTotalQuestions(questions.size());
        response.setTotalPoints(totalPoints);
        response.setStartedAt(savedAttempt.getStartedAt());
        
        if (quiz.getTimeLimitMinutes() != null) {
            response.setExpiresAt(savedAttempt.getStartedAt().plusMinutes(quiz.getTimeLimitMinutes()));
        }
        
        response.setQuestions(questionDTOs);
        
        logger.info("✅ Attempt {} started successfully", savedAttempt.getAttemptId());
        return response;
    }
    
    /**
     * Save nhiều answers cùng lúc
     */
    @Transactional
    public void saveBulkAnswers(Integer studentId, Integer attemptId, List<SubmitAnswerDTO> answers) {
        logger.info("💾 Saving {} answers for attempt {}", answers.size(), attemptId);
        
        // Verify attempt
        QuizAttempt attempt = attemptRepository.findByAttemptIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new RuntimeException("Quiz attempt not found or access denied"));
        
        // Verify status
        if (attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
            throw new RuntimeException("Quiz already completed. Cannot save answers.");
        }
        
        // Save each answer
        int savedCount = 0;
        for (SubmitAnswerDTO dto : answers) {
            try {
                // Check if answer already exists
                Optional<QuizAnswer> existingAnswer = answerRepository
                        .findByAttemptIdAndQuestionId(attemptId, dto.getQuestionId());
                
                QuizAnswer answer;
                if (existingAnswer.isPresent()) {
                    // Update existing answer
                    answer = existingAnswer.get();
                    answer.setSelectedOptionId(dto.getSelectedOptionId());
                    answer.setAnsweredAt(LocalDateTime.now());
                    logger.info("🔄 Updating answer for question {}", dto.getQuestionId());
                } else {
                    // Create new answer
                    answer = new QuizAnswer();
                    answer.setAttemptId(attemptId);
                    answer.setQuestionId(dto.getQuestionId());
                    answer.setSelectedOptionId(dto.getSelectedOptionId());
                    answer.setAnsweredAt(LocalDateTime.now());
                    logger.info("➕ Creating new answer for question {}", dto.getQuestionId());
                }
                
                // Check if correct
                QuizOption selectedOption = optionRepository.findById(dto.getSelectedOptionId())
                        .orElseThrow(() -> new RuntimeException("Option not found: " + dto.getSelectedOptionId()));
                
                boolean isCorrect = selectedOption.getIsCorrect();
                answer.setIsCorrect(isCorrect);
                
                answerRepository.save(answer);
                savedCount++;
                
            } catch (Exception e) {
                logger.error("❌ Error saving answer for question {}: {}", dto.getQuestionId(), e.getMessage());
                // Continue saving other answers
            }
        }
        
        logger.info("✅ Saved {}/{} answers successfully", savedCount, answers.size());
    }
    
    /**
     * Submit quiz và tính điểm
     */
    @Transactional
    public QuizResultDTO submitQuiz(Integer studentId, Integer attemptId) {
        logger.info("📝 Submitting quiz for attempt {}", attemptId);
        
        // Get attempt
        QuizAttempt attempt = attemptRepository.findByAttemptIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Quiz is not in progress");
        }
        
        // Get quiz
        Quiz quiz = quizRepository.findByQuizIdAndDeletedFalse(attempt.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        // Get all answers
        List<QuizAnswer> answers = answerRepository.findByAttemptIdOrderByAnsweredAt(attemptId);
        
        // Calculate score
        int correctCount = 0;
        int totalPoints = 0;
        
        for (QuizAnswer answer : answers) {
            if (answer.getIsCorrect()) {
                correctCount++;
                // Get question points
                QuizQuestion question = questionRepository.findById(answer.getQuestionId())
                        .orElse(null);
                if (question != null) {
                    totalPoints += question.getPoints();
                }
            }
        }
        
        BigDecimal score = new BigDecimal(totalPoints);
        int percentage = (int) ((totalPoints * 100.0) / attempt.getTotalPoints());
        
        // Determine status
        QuizAttemptStatus status = percentage >= quiz.getPassingScore() ? 
                QuizAttemptStatus.PASSED : QuizAttemptStatus.FAILED;
        
        // Calculate time spent
        LocalDateTime now = LocalDateTime.now();
        long timeSpentSeconds = java.time.Duration.between(attempt.getStartedAt(), now).getSeconds();
        
        // Update attempt
        attempt.setScore(score);
        attempt.setStatus(QuizAttemptStatus.COMPLETED);
        attempt.setCompletedAt(now);
        attempt.setTimeSpentSeconds((int) timeSpentSeconds);
        attemptRepository.save(attempt);
        
        // Update lesson progress nếu PASS
        if (status == QuizAttemptStatus.PASSED) {
            updateLessonProgress(studentId, quiz.getLessonId());
        }
        
        // Build result
        return buildQuizResult(attempt, quiz, answers, correctCount, percentage);
    }
    
    /**
     * Cập nhật lesson progress khi pass quiz
     */
    private void updateLessonProgress(Integer studentId, Integer lessonId) {
        Optional<LessonProgress> existing = lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId);
        
        LessonProgress progress;
        if (existing.isPresent()) {
            progress = existing.get();
        } else {
            progress = new LessonProgress();
            progress.setStudentId(studentId);
            progress.setLessonId(lessonId);
            progress.setProgressPercentage(0);
            progress.setTimeSpentSeconds(0);
            progress.setLastWatchedPosition(0);
        }
        
        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setProgressPercentage(100);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());
        
        lessonProgressRepository.save(progress);
        logger.info("✅ Updated lesson progress to COMPLETED for student {} lesson {}", studentId, lessonId);
    }
    
    /**
     * Build quiz result DTO
     */
    private QuizResultDTO buildQuizResult(QuizAttempt attempt, Quiz quiz, List<QuizAnswer> answers, int correctCount, int percentage) {
        QuizResultDTO result = new QuizResultDTO();
        result.setAttemptId(attempt.getAttemptId());
        result.setScore(attempt.getScore());
        result.setTotalPoints(attempt.getTotalPoints());
        result.setPercentage(percentage);
        result.setStatus(attempt.getStatus().name());
        result.setCompletedAt(attempt.getCompletedAt());
        result.setTimeSpentSeconds(attempt.getTimeSpentSeconds());
        result.setCorrectAnswers(correctCount);
        
        // Get total questions
        List<QuizQuestion> allQuestions = questionRepository.findByQuizIdOrderByQuestionOrder(quiz.getQuizId());
        result.setTotalQuestions(allQuestions.size());
        
        result.setIsPassed(attempt.getStatus() == QuizAttemptStatus.PASSED);
        
        // Check can retake
        List<QuizAttempt> allAttempts = attemptRepository.findByStudentIdAndQuizIdOrderByStartedAtDesc(attempt.getStudentId(), quiz.getQuizId());
        int completedCount = 0;
        for (QuizAttempt a : allAttempts) {
            if (a.getStatus() == QuizAttemptStatus.COMPLETED) {
                completedCount++;
            }
        }
        
        result.setCanRetake(completedCount < quiz.getMaxAttempts());
        result.setAttemptsRemaining(quiz.getMaxAttempts() - completedCount);
        
        // Build detailed results (always show after completion)
        if (true) {
            List<QuestionResultDTO> questionResults = new ArrayList<>();
            
            for (QuizAnswer answer : answers) {
                QuizQuestion question = questionRepository.findById(answer.getQuestionId()).orElse(null);
                if (question == null) continue;
                
                QuizOption selectedOption = optionRepository.findById(answer.getSelectedOptionId()).orElse(null);
                QuizOption correctOption = optionRepository.findByQuestionIdAndIsCorrectTrue(question.getQuestionId()).orElse(null);
                
                QuestionResultDTO qResult = new QuestionResultDTO();
                qResult.setQuestionId(question.getQuestionId());
                qResult.setQuestionText(question.getQuestionText());
                qResult.setSelectedOptionId(answer.getSelectedOptionId());
                qResult.setSelectedOptionText(selectedOption != null ? selectedOption.getOptionText() : "");
                qResult.setIsCorrect(answer.getIsCorrect());
                qResult.setCorrectOptionText(correctOption != null ? correctOption.getOptionText() : "");
                qResult.setPoints(answer.getIsCorrect() ? question.getPoints() : 0);
                
                questionResults.add(qResult);
            }
            
            result.setResults(questionResults);
        }
        
        return result;
    }
    
    /**
     * Lấy lịch sử làm bài
     */
    public AttemptHistoryDTO getAttemptHistory(Integer studentId, Integer quizId) {
        Quiz quiz = quizRepository.findByQuizIdAndDeletedFalse(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        List<QuizAttempt> attempts = attemptRepository.findByStudentIdAndQuizIdOrderByStartedAtDesc(studentId, quizId);
        
        List<AttemptSummaryDTO> summaries = new ArrayList<>();
        BigDecimal bestScore = BigDecimal.ZERO;
        int attemptNumber = 0;
        int completedCount = 0;
        
        for (QuizAttempt attempt : attempts) {
            if (attempt.getStatus() == QuizAttemptStatus.COMPLETED) {
                completedCount++;
                
                AttemptSummaryDTO summary = new AttemptSummaryDTO();
                summary.setAttemptId(attempt.getAttemptId());
                summary.setAttemptNumber(completedCount);
                summary.setScore(attempt.getScore());
                
                int percentage = (int) ((attempt.getScore().doubleValue() * 100) / attempt.getTotalPoints());
                summary.setPercentage(percentage);
                summary.setStatus(attempt.getStatus().name());
                summary.setStartedAt(attempt.getStartedAt());
                summary.setCompletedAt(attempt.getCompletedAt());
                summary.setTimeSpentSeconds(attempt.getTimeSpentSeconds());
                
                summaries.add(summary);
                
                if (attempt.getScore().compareTo(bestScore) > 0) {
                    bestScore = attempt.getScore();
                }
            }
        }
        
        AttemptHistoryDTO history = new AttemptHistoryDTO();
        history.setQuizId(quizId);
        history.setQuizTitle(quiz.getQuizTitle());
        history.setMaxAttempts(quiz.getMaxAttempts());
        history.setAttempts(summaries);
        history.setBestScore(bestScore);
        history.setCanRetake(completedCount < quiz.getMaxAttempts());
        history.setAttemptsRemaining(quiz.getMaxAttempts() - completedCount);
        
        return history;
    }
    
    public QuizResultDTO getAttemptResult(Integer attemptId, Integer studentId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        // Verify ownership
        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized access to attempt");
        }
        
        if (attempt.getStatus() != QuizAttemptStatus.COMPLETED) {
            throw new RuntimeException("Attempt not completed yet");
        }
        
        Quiz quiz = quizRepository.findByQuizIdAndDeletedFalse(attempt.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        // Get all answers for this attempt
        List<QuizAnswer> answers = answerRepository.findByAttemptIdOrderByAnsweredAt(attemptId);
        Map<Integer, QuizAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, a -> a));
        
        // Get all questions
        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByQuestionOrder(attempt.getQuizId());
        List<QuestionResultDTO> results = new ArrayList<>();
        
        for (QuizQuestion question : questions) {
            QuestionResultDTO result = new QuestionResultDTO();
            result.setQuestionId(question.getQuestionId());
            result.setQuestionText(question.getQuestionText());
            result.setPoints(question.getPoints());
            
            QuizAnswer answer = answerMap.get(question.getQuestionId());
            if (answer != null) {
                result.setSelectedOptionId(answer.getSelectedOptionId());
                
                // Get selected option text
                QuizOption selectedOption = optionRepository.findById(answer.getSelectedOptionId())
                        .orElse(null);
                if (selectedOption != null) {
                    result.setSelectedOptionText(selectedOption.getOptionText());
                    result.setIsCorrect(selectedOption.getIsCorrect());
                }
            }
            
            // Get correct option text
            List<QuizOption> options = optionRepository.findByQuestionIdOrderByOptionOrder(question.getQuestionId());
            for (QuizOption opt : options) {
                if (opt.getIsCorrect()) {
                    result.setCorrectOptionText(opt.getOptionText());
                    break;
                }
            }
            
            results.add(result);
        }
        
        // Build result DTO
        QuizResultDTO resultDTO = new QuizResultDTO();
        resultDTO.setAttemptId(attemptId);
        resultDTO.setScore(attempt.getScore());
        resultDTO.setTotalPoints(attempt.getTotalPoints());
        
        int percentage = (int) ((attempt.getScore().doubleValue() * 100) / attempt.getTotalPoints());
        resultDTO.setPercentage(percentage);
        resultDTO.setStatus(attempt.getStatus().name());
        resultDTO.setCompletedAt(attempt.getCompletedAt());
        resultDTO.setTimeSpentSeconds(attempt.getTimeSpentSeconds());
        
        long correctCount = results.stream().filter(QuestionResultDTO::getIsCorrect).count();
        resultDTO.setCorrectAnswers((int) correctCount);
        resultDTO.setTotalQuestions(questions.size());
        resultDTO.setIsPassed(percentage >= quiz.getPassingScore());
        
        int completedAttempts = attemptRepository.countByStudentIdAndQuizIdAndStatus(
                studentId, quiz.getQuizId(), QuizAttemptStatus.COMPLETED);
        resultDTO.setCanRetake(completedAttempts < quiz.getMaxAttempts());
        resultDTO.setAttemptsRemaining(quiz.getMaxAttempts() - completedAttempts);
        
        resultDTO.setResults(results);
        
        return resultDTO;
    }
}
