package com.example.sis.controller;

import com.example.sis.dto.chat.ChatMessageRequest;
import com.example.sis.dto.chat.ChatMessageResponse;
import com.example.sis.dto.chat.ChatSessionDTO;
import com.example.sis.entity.ChatMessage;
import com.example.sis.entity.ChatSession;
import com.example.sis.entity.MessageSource;
import com.example.sis.security.SecurityContextResolver;
import com.example.sis.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat Controller - AI Chatbot with RAG
 * RESTful API design:
 * - Session Management: /api/chat/sessions
 * - Chat Messaging: /api/chat/sessions/{sessionId}/messages
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()") // ✅ Require authentication for ALL endpoints
public class ChatController {
    
    private final ChatService chatService;
    private final SecurityContextResolver securityContext;
    
    // ==================== SESSION MANAGEMENT ====================
    
    /**
     * Create new chat session
     * POST /api/chat/sessions
     */
    @PostMapping("/sessions")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN', 'ACADEMIC_STAFF', 'LECTURER')")
    public ResponseEntity<ChatSessionDTO> createSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {
        Integer userId = securityContext.getCurrentUserId();
        String email = securityContext.getCurrentUserEmail();
        
        String title = request.getOrDefault("title", "New Chat");
        
        ChatSession session = chatService.createNewSession(userId, title);
        
        log.info("✨ User {} ({}) created new session {}", userId, email, session.getSessionId());
        
        return ResponseEntity.ok(toDTO(session));
    }
    
    /**
     * Get all chat sessions for current user
     * GET /api/chat/sessions
     * ✅ Users can only see THEIR OWN sessions
     */
    @GetMapping("/sessions")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN', 'ACADEMIC_STAFF', 'LECTURER')")
    public ResponseEntity<List<ChatSessionDTO>> getSessions(@AuthenticationPrincipal Jwt jwt) {
        Integer userId = securityContext.getCurrentUserId();
        
        log.info("📋 User {} requests their sessions", userId);
        
        List<ChatSession> sessions = chatService.getUserSessions(userId);
        
        List<ChatSessionDTO> dtos = sessions.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get session details with all messages
     * GET /api/chat/sessions/{sessionId}
     * ✅ Verify user owns this session
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN', 'ACADEMIC_STAFF', 'LECTURER')")
    public ResponseEntity<Map<String, Object>> getSessionDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer sessionId) {
        Integer userId = securityContext.getCurrentUserId();
        
        ChatSession session = chatService.getSession(sessionId);
        
        // ✅ CRITICAL: Verify ownership (only admin or owner can view)
        if (!session.getUserId().equals(userId) && !securityContext.isAdmin()) {
            log.warn("🚫 User {} tried to access session {} owned by user {}", 
                userId, sessionId, session.getUserId());
            return ResponseEntity.status(403).build();
        }
        
        ChatSessionDTO sessionDTO = toDTO(session);
        List<ChatMessage> messages = chatService.getConversationHistory(session, 100);
        
        // Convert messages to response DTOs
        List<ChatMessageResponse> messageResponses = messages.stream()
            .map(this::toMessageResponse)
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("session", sessionDTO);
        response.put("messages", messageResponses);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update session title
     * PUT /api/chat/sessions/{sessionId}/title
     */
    @PutMapping("/sessions/{sessionId}/title")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN', 'ACADEMIC_STAFF', 'LECTURER')")
    public ResponseEntity<Void> updateSessionTitle(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Integer sessionId,
        @RequestBody Map<String, String> request
    ) {
        Integer userId = securityContext.getCurrentUserId();
        String newTitle = request.get("title");
        
        if (newTitle == null || newTitle.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        ChatSession session = chatService.getSession(sessionId);
        
        // ✅ Verify ownership
        if (!session.getUserId().equals(userId) && !securityContext.isAdmin()) {
            log.warn("🚫 User {} tried to update session {} owned by user {}", 
                userId, sessionId, session.getUserId());
            return ResponseEntity.status(403).build();
        }
        
        chatService.updateSessionTitle(sessionId, newTitle);
        
        log.info("✏️ User {} updated session {} title to: {}", userId, sessionId, newTitle);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete a session
     * DELETE /api/chat/sessions/{sessionId}
     * ✅ Only owner or admin can delete
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN', 'ACADEMIC_STAFF', 'LECTURER')")
    public ResponseEntity<Void> deleteSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer sessionId) {
        Integer userId = securityContext.getCurrentUserId();
        
        ChatSession session = chatService.getSession(sessionId);
        
        // ✅ Verify ownership
        if (!session.getUserId().equals(userId) && !securityContext.isAdmin()) {
            log.warn("🚫 User {} tried to delete session {} owned by user {}", 
                userId, sessionId, session.getUserId());
            return ResponseEntity.status(403).build();
        }
        
        chatService.deleteSession(sessionId, userId);
        
        log.info("🗑️ User {} deleted session {}", userId, sessionId);
        
        return ResponseEntity.ok().build();
    }
    
    // ==================== CHAT MESSAGING ====================
    
    /**
     * Send message in a session and get AI response
     * POST /api/chat/sessions/{sessionId}/messages
     * ✅ All authenticated users can send messages
     */
    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN', 'ACADEMIC_STAFF', 'LECTURER')")
    public Mono<ResponseEntity<ChatMessageResponse>> sendMessage(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Integer sessionId,
        @RequestBody @jakarta.validation.Valid ChatMessageRequest request
    ) {
        // ✅ Get REAL user ID from JWT token
        Integer userId = securityContext.getCurrentUserId();
        String username = securityContext.getCurrentUsername();
        
        // DEBUG: Log full request object
        log.info("📥 Received request: sessionId={}, message='{}', classId={}, moduleId={}", 
            request.getSessionId(), request.getMessage(), request.getClassId(), request.getModuleId());
        
        // Validate message is not null or empty
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            log.error("❌ Empty message received from user {}", userId);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        log.info("📨 User {} ({}) sends message to session {}: {}", 
            userId, username, sessionId, request.getMessage());
        
        long startTime = System.currentTimeMillis();
        
        // Step 1: Get session and verify ownership
        ChatSession session = chatService.getSession(sessionId);
        
        if (!session.getUserId().equals(userId) && !securityContext.isAdmin()) {
            log.warn("🚫 User {} tried to send message to session {} owned by user {}", 
                userId, sessionId, session.getUserId());
            return Mono.just(ResponseEntity.status(403).build());
        }
        
        // Step 2: Save user message (auto-sanitized)
        chatService.saveUserMessage(session, request.getMessage());
        
        // Step 3: Build SAFE context (NO TOKENS, NO PASSWORDS)
        Map<String, Object> context = securityContext.getSafeChatContext();
        log.info("👤 User context for chat: {}", context);
        
        // Add request-specific context
        if (request.getClassId() != null) context.put("classId", request.getClassId());
        if (request.getModuleId() != null) context.put("moduleId", request.getModuleId());
        if (request.getLessonId() != null) context.put("lessonId", request.getLessonId());
        
        // Step 4: Search relevant context (RAG) - auto-validated
        return chatService.searchRelevantContext(request.getMessage(), context)
            .flatMap(sources -> {
                log.info("📚 Found {} relevant sources for user {}", sources.size(), userId);
                
                // Step 5: Get conversation history
                List<ChatMessage> history = chatService.getConversationHistory(session, 5);
                
                // Step 6: Generate response with Cohere (auto-sanitized)
                return chatService.generateResponse(request.getMessage(), sources, history, context)
                    .map(responseText -> {
                        int completionMs = (int) (System.currentTimeMillis() - startTime);
                        
                        // Step 7: Save assistant message
                        chatService.saveAssistantMessage(
                            session, 
                            responseText, 
                            sources, 
                            completionMs
                        );
                        
                        // Build response
                        ChatMessageResponse response = new ChatMessageResponse();
                        response.setSessionId(session.getSessionId());
                        response.setMessage(responseText);
                        response.setSources(sources);
                        response.setCompletionMs(completionMs);
                        response.setTimestamp(LocalDateTime.now());
                        
                        log.info("✅ Response generated for user {} in {}ms", userId, completionMs);
                        
                        return ResponseEntity.ok(response);
                    });
            })
            .onErrorResume(error -> {
                log.error("❌ Failed to process message for user {}", userId, error);
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }
    
    /**
     * Get all messages in a session
     * GET /api/chat/sessions/{sessionId}/messages
     * ✅ Verify user owns this session
     */
    @GetMapping("/sessions/{sessionId}/history")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<ChatMessageResponse>> getSessionMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer sessionId) {
        Integer userId = securityContext.getCurrentUserId();
        
        ChatSession session = chatService.getSession(sessionId);
        
        // ✅ CRITICAL: Verify ownership (only admin or owner can view)
        if (!session.getUserId().equals(userId) && !securityContext.isAdmin()) {
            log.warn("🚫 User {} tried to access messages in session {} owned by user {}", 
                userId, sessionId, session.getUserId());
            return ResponseEntity.status(403).build();
        }
        
        List<ChatMessage> messages = chatService.getConversationHistory(session, 100);
        List<ChatMessageResponse> responses = messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    // ==================== STREAMING CHAT ====================
    
    /**
     * Send message in a session and get AI response via SSE (Server-Sent Events)
     * POST /api/chat/sessions/{sessionId}/messages/stream
     * ✅ Real-time streaming response
     * 
     * Response format: text/event-stream
     * Events:
     * - data: {"type":"TEXT","content":"Hello"}
     * - data: {"type":"TEXT","content":" world"}
     * - data: {"type":"DONE"}
     */
    @PostMapping(value = "/sessions/{sessionId}/messages/stream", produces = "text/event-stream")
    @PreAuthorize("@authz.hasAnyRole(authentication, 'STUDENT', 'TEACHER', 'ADMIN')")
    public Flux<String> sendMessageStream(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Integer sessionId,
        @RequestBody ChatMessageRequest request
    ) {
        // ✅ Get REAL user ID from JWT token
        Integer userId = securityContext.getCurrentUserId();
        String username = securityContext.getCurrentUsername();
        
        log.info("📨🌊 User {} ({}) streams message to session {}: {}", 
            userId, username, sessionId, request.getMessage());
        
        // Step 1: Verify ownership
        ChatSession session;
        try {
            session = chatService.getSession(sessionId);
            
            if (!session.getUserId().equals(userId) && !securityContext.isAdmin()) {
                log.warn("🚫 User {} tried to stream to session {} owned by user {}", 
                    userId, sessionId, session.getUserId());
                return Flux.just("data: {\"type\":\"ERROR\",\"error\":\"Access denied\"}\n\n");
            }
        } catch (Exception e) {
            log.error("Failed to get session", e);
            return Flux.just("data: {\"type\":\"ERROR\",\"error\":\"Session not found\"}\n\n");
        }
        
        // Step 2: Save user message (auto-sanitized)
        chatService.saveUserMessage(session, request.getMessage());
        
        // Step 3: Build SAFE context (NO TOKENS, NO PASSWORDS)
        Map<String, Object> context = securityContext.getSafeChatContext();
        
        // Add request-specific context
        if (request.getClassId() != null) context.put("classId", request.getClassId());
        if (request.getModuleId() != null) context.put("moduleId", request.getModuleId());
        if (request.getLessonId() != null) context.put("lessonId", request.getLessonId());
        
        // Step 4: Search relevant context (RAG)
        return chatService.searchRelevantContext(request.getMessage(), context)
            .flatMapMany(sources -> {
                log.info("📚 Found {} relevant sources", sources.size());
                
                // Step 5: Get conversation history
                List<ChatMessage> history = chatService.getConversationHistory(session, 5);
                
                // Step 6: Stream response chunks
                long startTime = System.currentTimeMillis();
                StringBuilder fullResponse = new StringBuilder();
                
                return chatService.generateResponseStream(request.getMessage(), sources, history, context)
                    .map(chunk -> {
                        fullResponse.append(chunk);
                        // Format as SSE: data: {...}\n\n
                        return "data: {\"type\":\"TEXT\",\"content\":" + 
                               escapeJson(chunk) + "}\n\n";
                    })
                    .concatWith(Flux.defer(() -> {
                        // After all chunks, save complete response and send DONE
                        int completionMs = (int) (System.currentTimeMillis() - startTime);
                        
                        // Step 7: Save assistant message
                        chatService.saveAssistantMessage(
                            session,
                            fullResponse.toString(),
                            sources,
                            completionMs
                        );
                        
                        log.info("✅ Stream completed in {}ms", completionMs);
                        
                        return Flux.just("data: {\"type\":\"DONE\",\"completionMs\":" + completionMs + "}\n\n");
                    }));
            })
            .onErrorResume(error -> {
                log.error("❌ Failed to stream response", error);
                return Flux.just("data: {\"type\":\"ERROR\",\"error\":\"" + 
                               escapeJson(error.getMessage()) + "\"}\n\n");
            });
    }
    
    // ===== Helper Methods =====
    
    private ChatSessionDTO toDTO(ChatSession session) {
        return ChatSessionDTO.builder()
            .sessionId(session.getSessionId())
            .title(session.getTitle())
            .context(session.getContext())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .messageCount(session.getMessages() != null ? session.getMessages().size() : 0)
            .build();
    }
    
    private ChatMessageResponse toResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setSessionId(message.getSession().getSessionId());
        response.setMessage(message.getContent());
        response.setSources(null); // TODO: implement sources from RAG context
        response.setCompletionMs(message.getCompletionMs());
        response.setTimestamp(message.getCreatedAt());
        return response;
    }
    
    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setSessionId(message.getSession().getSessionId());
        response.setRole(message.getRole().name()); // Convert enum to string
        response.setMessage(message.getContent()); // Map content -> message
        response.setSources(message.getSources()); // Include RAG sources
        response.setCompletionMs(message.getCompletionMs());
        response.setTimestamp(message.getCreatedAt()); // Map createdAt -> timestamp
        return response;
    }
    
    /**
     * Escape JSON string for SSE format
     */
    private String escapeJson(String text) {
        if (text == null) return "null";
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            + "\"";
    }
}
