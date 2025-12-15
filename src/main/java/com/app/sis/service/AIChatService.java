package com.app.sis.service;

import com.app.sis.dto.AIChatMessageDto;
import com.app.sis.dto.AIChatRequestDto;
import com.app.sis.dto.AIChatResponseDto;
import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {
    
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;
    
    // In-memory KV store for chat history (max 50 messages per user)
    private final Map<Long, LinkedList<AIChatMessageDto>> chatHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 50;
    
    // Rule-based patterns
    private static final Map<Pattern, List<String>> RULE_PATTERNS = new HashMap<>();
    
    static {
        // Greetings
        RULE_PATTERNS.put(
            Pattern.compile("(xin chào|chào|hello|hi|hey)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Xin chào {name}! Tôi là trợ lý AI của bạn. Tôi có thể giúp gì cho bạn hôm nay?",
                "Chào {name}! Rất vui được hỗ trợ bạn. Bạn cần tôi giúp gì không?",
                "Hi {name}! Tôi ở đây để hỗ trợ bạn. Có câu hỏi gì không?"
            )
        );
        
        // Questions about classes
        RULE_PATTERNS.put(
            Pattern.compile("(lớp học|môn học|khóa học|classes?|courses?)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Bạn có thể xem tất cả lớp học của mình trong mục 'Lớp học của tôi'. Bạn muốn biết thông tin gì cụ thể về lớp học?",
                "Để xem danh sách lớp học, hãy vào phần 'Lớp học của tôi'. Tôi có thể giúp bạn tìm hiểu về tiến độ học tập của bạn.",
                "Bạn đang tìm kiếm thông tin về lớp học nào? Tôi có thể giúp bạn tra cứu lịch học, bài tập hoặc điểm số."
            )
        );
        
        // Questions about grades/scores
        RULE_PATTERNS.put(
            Pattern.compile("(điểm|kết quả|grade|score|exam)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Bạn có thể xem điểm số của mình trong phần 'Kết quả học tập'. Bạn muốn xem điểm môn nào?",
                "Để kiểm tra điểm, hãy vào mục 'Điểm số' hoặc 'Kết quả thi'. Tôi có thể hướng dẫn bạn chi tiết hơn.",
                "Bạn có thể tra cứu điểm của tất cả các môn học trong hồ sơ học tập của mình. Cần tôi giúp gì thêm không?"
            )
        );
        
        // Questions about schedule
        RULE_PATTERNS.put(
            Pattern.compile("(lịch học|thời khóa biểu|schedule|timetable)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Lịch học của bạn có thể xem trong mục 'Lịch học' hoặc 'Thời khóa biểu'. Bạn muốn xem lịch ngày nào?",
                "Bạn có thể kiểm tra lịch học hàng tuần trong phần Lớp học của tôi. Cần tôi giúp tìm thông tin gì?",
                "Thời khóa biểu được cập nhật trong hệ thống. Bạn muốn xem lịch học môn nào?"
            )
        );
        
        // Questions about attendance
        RULE_PATTERNS.put(
            Pattern.compile("(điểm danh|attendance|vắng|absent)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Bạn có thể xem thông tin điểm danh trong mục 'Điểm danh' của từng lớp học. Bạn muốn kiểm tra lớp nào?",
                "Lịch sử điểm danh được cập nhật đầy đủ trong hệ thống. Bạn cần xem tỷ lệ tham gia lớp học nào?",
                "Để xem tình trạng điểm danh, vào phần chi tiết lớp học. Tôi có thể giúp gì thêm?"
            )
        );
        
        // Questions about assignments/homework
        RULE_PATTERNS.put(
            Pattern.compile("(bài tập|assignment|homework|deadline)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Bạn có thể xem bài tập và deadline trong mục 'Bài tập' của từng môn học. Cần tôi giúp tìm bài tập nào?",
                "Danh sách bài tập và thời hạn nộp được hiển thị trong phần Học tập. Bạn muốn xem môn nào?",
                "Tôi có thể giúp bạn theo dõi bài tập chưa hoàn thành. Bạn muốn kiểm tra môn học nào?"
            )
        );
        
        // General help
        RULE_PATTERNS.put(
            Pattern.compile("(giúp|help|hỗ trợ|support)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Tôi có thể giúp bạn với: \n- Tra cứu lịch học\n- Xem điểm số\n- Kiểm tra bài tập\n- Thông tin lớp học\nBạn cần tôi hỗ trợ về vấn đề gì?",
                "Tôi ở đây để hỗ trợ bạn! Bạn có thể hỏi tôi về lịch học, điểm số, bài tập, hoặc bất kỳ thông tin nào về học tập.",
                "Hãy cho tôi biết bạn cần giúp gì: lớp học, điểm số, lịch học, bài tập... Tôi sẵn sàng hỗ trợ!"
            )
        );
        
        // Thank you
        RULE_PATTERNS.put(
            Pattern.compile("(cảm ơn|thanks|thank you|cám ơn)", Pattern.CASE_INSENSITIVE),
            Arrays.asList(
                "Rất vui được giúp đỡ bạn! Nếu cần gì thêm, hãy hỏi tôi nhé.",
                "Không có gì! Tôi luôn sẵn sàng hỗ trợ bạn.",
                "Bạn cứ thoải mái hỏi tôi bất cứ lúc nào nhé!"
            )
        );
    }
    
    /**
     * Process chat message with dual response system
     */
    public AIChatResponseDto processMessage(AIChatRequestDto request) {
        Long userId = request.getUserId();
        String userName = request.getUserName() != null ? request.getUserName() : "bạn";
        
        // Add user message to history
        addToHistory(userId, "user", request.getMessage());
        
        String response;
        String responseType;
        
        // Try OpenAI if requested and API key is available
        if (request.isUseOpenAI() && openaiApiKey != null && !openaiApiKey.isEmpty()) {
            try {
                response = getOpenAIResponse(request.getMessage(), userName, userId);
                responseType = "openai";
                log.info("OpenAI response generated for user {}", userId);
            } catch (Exception e) {
                log.error("OpenAI API failed, falling back to rule-based", e);
                response = getRuleBasedResponse(request.getMessage(), userName, userId);
                responseType = "rule-based";
            }
        } else {
            // Default to rule-based
            response = getRuleBasedResponse(request.getMessage(), userName, userId);
            responseType = "rule-based";
        }
        
        // Add assistant response to history
        addToHistory(userId, "assistant", response);
        
        return new AIChatResponseDto(response, responseType, true);
    }
    
    /**
     * Get chat history for a user
     */
    public List<AIChatMessageDto> getChatHistory(Long userId) {
        return new ArrayList<>(chatHistory.getOrDefault(userId, new LinkedList<>()));
    }
    
    /**
     * Clear chat history for a user
     */
    public void clearChatHistory(Long userId) {
        chatHistory.remove(userId);
    }
    
    /**
     * Rule-based response with context-awareness
     */
    private String getRuleBasedResponse(String message, String userName, Long userId) {
        // Pattern matching
        for (Map.Entry<Pattern, List<String>> entry : RULE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(message).find()) {
                List<String> responses = entry.getValue();
                String response = responses.get(new Random().nextInt(responses.size()));
                return response.replace("{name}", userName);
            }
        }
        
        // Default response with personalization
        return String.format(
            "Xin chào %s! Tôi chưa hiểu rõ câu hỏi của bạn. " +
            "Bạn có thể hỏi tôi về lịch học, điểm số, bài tập, hoặc thông tin lớp học. " +
            "Tôi sẵn sàng hỗ trợ bạn!",
            userName
        );
    }
    
    /**
     * OpenAI GPT-3.5-turbo integration
     */
    private String getOpenAIResponse(String message, String userName, Long userId) {
        RestTemplate restTemplate = new RestTemplate();
        
        // Prepare system prompt with context
        String systemPrompt = String.format(
            "Bạn là trợ lý AI thông minh của hệ thống quản lý học sinh. " +
            "Tên học viên là %s. " +
            "Hãy trả lời câu hỏi một cách thân thiện, hữu ích và ngắn gọn. " +
            "Tập trung vào việc hướng dẫn học viên sử dụng hệ thống để tra cứu lớp học, điểm số, lịch học, và bài tập.",
            userName
        );
        
        // Build messages array
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        // Add recent history (last 5 messages for context)
        List<AIChatMessageDto> history = getChatHistory(userId);
        int startIndex = Math.max(0, history.size() - 5);
        for (int i = startIndex; i < history.size(); i++) {
            AIChatMessageDto msg = history.get(i);
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }
        
        // Add current message
        messages.add(Map.of("role", "user", "content", message));
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 300);
        requestBody.put("temperature", 0.7);
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Call OpenAI API
        ResponseEntity<Map> response = restTemplate.exchange(
            openaiApiUrl,
            HttpMethod.POST,
            entity,
            Map.class
        );
        
        // Parse response
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, String> messageObj = (Map<String, String>) firstChoice.get("message");
                return messageObj.get("content");
            }
        }
        
        throw new RuntimeException("Invalid OpenAI response");
    }
    
    /**
     * Add message to chat history (keep last 50 messages)
     */
    private void addToHistory(Long userId, String role, String content) {
        chatHistory.putIfAbsent(userId, new LinkedList<>());
        LinkedList<AIChatMessageDto> history = chatHistory.get(userId);
        
        history.add(new AIChatMessageDto(role, content, LocalDateTime.now()));
        
        // Keep only last 50 messages
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeFirst();
        }
    }
}
