package com.example.sis.service.chat;

import com.example.sis.entity.ChatMessage;
import com.example.sis.entity.ChatSession;
import com.example.sis.entity.MessageSource;
import com.example.sis.repository.ChatMessageRepository;
import com.example.sis.repository.ChatSessionRepository;
import com.example.sis.service.cohere.CohereService;
import com.example.sis.service.vector.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chat Service with RAG (Retrieval Augmented Generation)
 * Uses Cohere for embeddings and chat generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CohereService cohereService;
    private final QdrantService qdrantService;
    private final SecurityAuditService securityAuditService;
    private final RealtimeDataFetcher realtimeDataFetcher;
    
    /**
     * Get or create chat session
     */
    @Transactional
    public ChatSession getOrCreateSession(Integer sessionId, Integer userId, String firstMessage) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        }
        
        // Create new session
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(generateTitle(firstMessage));
        session.setContext(new HashMap<>());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        return sessionRepository.save(session);
    }
    
    /**
     * Save user message (with sanitization)
     */
    @Transactional
    public ChatMessage saveUserMessage(ChatSession session, String content) {
        // ✅ Sanitize user input before saving
        String sanitizedContent = securityAuditService.sanitizeUserMessage(content);
        
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(ChatMessage.MessageRole.user);
        message.setContent(sanitizedContent);
        message.setCreatedAt(LocalDateTime.now());
        
        log.debug("💾 Saved user message: {} chars", sanitizedContent.length());
        
        return messageRepository.save(message);
    }
    
    /**
     * Save assistant message
     */
    @Transactional
    public ChatMessage saveAssistantMessage(ChatSession session, String content, 
                                           List<MessageSource> sources, 
                                           Integer completionMs) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(ChatMessage.MessageRole.assistant);
        message.setContent(content);
        message.setSources(sources);
        message.setCompletionMs(completionMs);
        message.setModel("command-r-plus"); // Cohere model
        message.setCreatedAt(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    /**
     * Perform RAG search to find relevant context
     */
    public Mono<List<MessageSource>> searchRelevantContext(String query, Map<String, Object> context) {
        log.info("🔍 Searching relevant context for query: {}", query);
        
        // ✅ CRITICAL: Validate context has NO sensitive data
        securityAuditService.validateSafeContext(context);
        
        return cohereService.createQueryEmbedding(query)
            .flatMap(queryVector -> {
                // Build filter from context
                Map<String, Object> filter = new HashMap<>();
                if (context != null) {
                    if (context.containsKey("classId")) {
                        filter.put("class_id", context.get("classId"));
                    }
                    if (context.containsKey("moduleId")) {
                        filter.put("module_id", context.get("moduleId"));
                    }
                }
                
                // Search in Qdrant (now reactive)
                return qdrantService.searchSimilar(
                    queryVector,
                    10,          // limit: top 10 chunks (more context for AI)
                    0.3,         // threshold: similarity > 0.3 (prioritize recall over precision)
                    filter
                ).doOnSuccess(sources -> log.info("✅ Found {} relevant chunks", sources.size()));
            })
            .onErrorResume(error -> {
                log.error("❌ Failed to search context", error);
                return Mono.just(List.of());
            });
    }
    
    /**
     * Generate response using Cohere Chat API with RAG context (non-streaming)
     */
    public Mono<String> generateResponse(String userMessage, List<MessageSource> sources, 
                                        List<ChatMessage> conversationHistory,
                                        Map<String, Object> userContext) {
        log.info("🤖 Generating response with {} sources for user {}", sources.size(), userContext.get("userId"));
        
        // Build context from sources
        String context = buildContextFromSources(sources);
        
        // 🔥 NEW: Inject real-time data into context
        context = realtimeDataFetcher.buildContextWithRealtimeData(userMessage, context);
        
        // Build conversation history
        String history = buildConversationHistory(conversationHistory);
        
        // Build user profile context
        String userProfile = buildUserProfile(userContext);
        
        // DEBUG: Log user context and profile
        log.info("🔍 DEBUG User Context: {}", userContext);
        log.info("📋 DEBUG User Profile:\n{}", userProfile);
        
        // Build prompt for streaming
        String systemPrompt = """
            Bạn là trợ lý AI của CodeGym, giúp sinh viên học lập trình.
            
            ⚠️ QUY TẮC BẮT BUỘC (KHÔNG ĐƯỢC VI PHẠM):
            - CHỈ trả lời dựa trên CONTEXT TÀI LIỆU và THÔNG TIN NGƯỜI DÙNG bên dưới
            - KHÔNG được bịa thêm thông tin không có trong context (ví dụ: tên trung tâm, cổng thanh toán, số tiền)
            - Nếu context KHÔNG CÓ thông tin, hãy nói "Hiện tại tôi chưa có thông tin này trong tài liệu"
            - KHÔNG được đoán hoặc suy luận thông tin không có trong context
            
            THÔNG TIN NGƯỜI DÙNG:
            %s
            
            CONTEXT TÀI LIỆU:
            %s
            
            CÁCH TRẢ LỜI:
            1. Đọc kỹ CONTEXT TÀI LIỆU bên trên trước khi trả lời
            2. Nếu câu hỏi về thông tin cá nhân ("tôi học lớp nào", "lịch học của tôi"), dùng THÔNG TIN NGƯỜI DÙNG
            3. Nếu câu hỏi về quy định, học phí, chính sách → dùng CONTEXT TÀI LIỆU
            4. Trả lời trực tiếp, chi tiết, trích dẫn CHÍNH XÁC từ context (số tiền, phần trăm, tên chính thức)
            5. TUYỆT ĐỐI không nhắc đến: "Trung tâm Hà Nội 5", "cổng thanh toán SIS", hoặc BẤT KỲ thông tin nào KHÔNG CÓ trong context
            
            LỊCH SỬ HỘI THOẠI:
            %s
            """.formatted(userProfile, context, history);
        
        String fullPrompt = systemPrompt + "\n\nCÂU HỎI: " + userMessage + "\n\nTRẢ LỜI:";
        
        return cohereService.generateText(fullPrompt)
            .map(response -> {
                // ✅ Sanitize LLM response (prevent leaking sensitive data)
                String sanitized = securityAuditService.sanitizeLLMResponse(response);
                log.info("✅ Generated response: {} chars", sanitized.length());
                return sanitized;
            })
            .doOnError(error -> log.error("❌ Failed to generate response", error));
    }
    
    /**
     * Generate response using Cohere Chat API with RAG context (streaming)
     * @return Flux of text chunks for SSE
     */
    public Flux<String> generateResponseStream(String userMessage, List<MessageSource> sources, 
                                               List<ChatMessage> conversationHistory,
                                               Map<String, Object> userContext) {
        log.info("🌊 Streaming response with {} sources for user {}", sources.size(), userContext.get("userId"));
        
        // Build context from sources
        String context = buildContextFromSources(sources);
        
        // 🔥 NEW: Inject real-time data into context
        context = realtimeDataFetcher.buildContextWithRealtimeData(userMessage, context);
        
        // Build conversation history
        String history = buildConversationHistory(conversationHistory);
        
        // Build user profile context
        String userProfile = buildUserProfile(userContext);
        
        // DEBUG: Log user context and profile
        log.info("🔍 DEBUG User Context: {}", userContext);
        log.info("📋 DEBUG User Profile:\n{}", userProfile);
        
        // Build prompt
        String systemPrompt = """
            Bạn là trợ lý AI của CodeGym, giúp sinh viên học lập trình.
            
            ⚠️ QUY TẮC BẮT BUỘC (KHÔNG ĐƯỢC VI PHẠM):
            1. ✅ ƯU TIÊN CAO NHẤT: "=== DỮ LIỆU DATABASE REALTIME ==="
               → Nếu thấy phần này, BẮT BUỘC phải dùng dữ liệu này để trả lời
               → Đây là dữ liệu thật từ database, luôn chính xác nhất
            
            2. Ưu tiên thấp hơn: "=== TÀI LIỆU THAM KHẢO ==="
               → CHỈ dùng khi KHÔNG có "DỮ LIỆU DATABASE REALTIME"
               → Đây là tài liệu cũ, có thể lỗi thời
            
            3. Nếu cả 2 đều không có thông tin → Trả lời: "Hiện tại tôi chưa có thông tin này"
            
            THÔNG TIN NGƯỜI DÙNG:
            %s
            
            CONTEXT (Ưu tiên từ trên xuống):
            %s
            
            CÁCH TRẢ LỜI:
            - Tìm "=== DỮ LIỆU DATABASE REALTIME ===" trong context
            - Nếu TÌM THẤY:
              1. Tìm dòng "📅 NGÀY HÔM NAY: YYYY-MM-DD" → Đây là ngày hôm nay CHÍNH XÁC
              2. Dùng SỐ LIỆU đã tính sẵn (VD: "45 ngày"), TUYỆT ĐỐI KHÔNG tự tính lại
              3. Khi trả lời phải nói rõ "tính từ hôm nay (ngày YYYY-MM-DD)"
            - Nếu KHÔNG TÌM THẤY → Dùng "=== TÀI LIỆU THAM KHẢO ==="
            - ⚠️ NGHIÊM CẤM: Không được tự nghĩ ngày hôm nay, phải dùng ngày trong section REALTIME
            
            LỊCH SỬ HỘI THOẠI:
            %s
            """.formatted(userProfile, context, history);
        
        String fullPrompt = systemPrompt + "\n\nCÂU HỎI: " + userMessage + "\n\nTRẢ LỜI:";
        
        return cohereService.generateTextStream(fullPrompt)
            .map(chunk -> {
                // ✅ Sanitize each chunk (prevent leaking sensitive data)
                return securityAuditService.sanitizeLLMResponse(chunk);
            })
            .doOnComplete(() -> log.info("✅ Streaming completed"))
            .doOnError(error -> log.error("❌ Failed to stream response", error));
    }
    
    /**
     * Get conversation history for context
     */
    public List<ChatMessage> getConversationHistory(ChatSession session, int limit) {
        return messageRepository.findBySessionOrderByCreatedAtDesc(session)
            .stream()
            .limit(limit)
            .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
            .collect(Collectors.toList());
    }
    
    /**
     * Get session by ID
     */
    public ChatSession getSession(Integer sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }
    
    /**
     * Create new empty session
     */
    @Transactional
    public ChatSession createNewSession(Integer userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setContext(new HashMap<>());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        ChatSession savedSession = sessionRepository.save(session);
        log.info("✨ Created new session {} for user {}", savedSession.getSessionId(), userId);
        
        return savedSession;
    }
    
    /**
     * Update session title
     */
    @Transactional
    public void updateSessionTitle(Integer sessionId, String newTitle) {
        ChatSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        session.setTitle(newTitle);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        log.info("✏️ Updated session {} title to: {}", sessionId, newTitle);
    }
    
    /**
     * Get all sessions for user
     */
    public List<ChatSession> getUserSessions(Integer userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    /**
     * Delete session
     */
    @Transactional
    public void deleteSession(Integer sessionId, Integer userId) {
        ChatSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        sessionRepository.delete(session);
    }
    
    // ===== Helper Methods =====
    
    private String generateTitle(String firstMessage) {
        return firstMessage.length() > 50 
            ? firstMessage.substring(0, 50) + "..." 
            : firstMessage;
    }
    
    private String buildContextFromSources(List<MessageSource> sources) {
        if (sources.isEmpty()) {
            return "(Không tìm thấy tài liệu liên quan)";
        }
        
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            MessageSource source = sources.get(i);
            context.append(String.format("[%d] %s\nĐộ tương đồng: %.2f\nNội dung: %s\n\n", 
                i + 1, source.getTitle(), source.getSimilarity(), source.getExcerpt()));
        }
        
        return context.toString();
    }
    
    private String buildConversationHistory(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "(Đây là câu hỏi đầu tiên)";
        }
        
        StringBuilder history = new StringBuilder();
        for (ChatMessage msg : messages) {
            String role = msg.getRole() == ChatMessage.MessageRole.user ? "Sinh viên" : "Trợ lý";
            history.append(String.format("%s: %s\n", role, msg.getContent()));
        }
        
        return history.toString();
    }
    
    private String buildUserProfile(Map<String, Object> userContext) {
        if (userContext == null || userContext.isEmpty()) {
            return "(Không có thông tin người dùng)";
        }
        
        StringBuilder profile = new StringBuilder();
        
        // Basic info
        if (userContext.containsKey("fullName")) {
            profile.append("- Họ tên: ").append(userContext.get("fullName")).append("\n");
        }
        
        if (userContext.containsKey("email")) {
            profile.append("- Email: ").append(userContext.get("email")).append("\n");
        }
        
        if (userContext.containsKey("phone")) {
            profile.append("- Điện thoại: ").append(userContext.get("phone")).append("\n");
        }
        
        if (userContext.containsKey("studentId")) {
            profile.append("- Mã sinh viên: ").append(userContext.get("studentId")).append("\n");
        }
        
        if (userContext.containsKey("status")) {
            String status = userContext.get("status").toString();
            String statusVietnamese = switch (status) {
                case "PENDING" -> "Chờ xử lý";
                case "STUDYING" -> "Đang học";
                case "GRADUATED" -> "Đã tốt nghiệp";
                case "DROPPED_OUT" -> "Đã bỏ học";
                case "SUSPENDED" -> "Tạm ngưng";
                default -> status;
            };
            profile.append("- Trạng thái: ").append(statusVietnamese).append("\n");
        }
        
        // Show classes with full details (schedule, room, program)
        if (userContext.containsKey("classes")) {
            List<?> classes = (List<?>) userContext.get("classes");
            if (!classes.isEmpty()) {
                profile.append("- Lớp học đang theo học:\n");
                for (Object obj : classes) {
                    if (obj instanceof Map) {
                        Map<?, ?> classInfo = (Map<?, ?>) obj;
                        profile.append("  + ").append(classInfo.get("className"))
                               .append(" (ID: ").append(classInfo.get("classId")).append(")\n");
                        profile.append("    - Chương trình: ").append(classInfo.get("programName")).append("\n");
                        profile.append("    - Trung tâm: ").append(classInfo.get("centerName")).append("\n");
                        profile.append("    - Trạng thái lớp: ").append(classInfo.get("status")).append("\n");
                        profile.append("    - Thời gian: ").append(classInfo.get("startDate"))
                               .append(" đến ").append(classInfo.get("endDate")).append("\n");
                        profile.append("    - Lịch học: ").append(classInfo.get("studyDays"))
                               .append(", ").append(classInfo.get("studyTime")).append("\n");
                        profile.append("    - Phòng học: ").append(classInfo.get("room")).append("\n");
                    }
                }
            }
        }
        
        if (userContext.containsKey("roles")) {
            profile.append("- Vai trò trong hệ thống: ").append(userContext.get("roles")).append("\n");
        }
        
        return profile.toString();
    }
}
