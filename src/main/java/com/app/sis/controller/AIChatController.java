package com.app.sis.controller;

import com.app.sis.dto.AIChatMessageDto;
import com.app.sis.dto.AIChatRequestDto;
import com.app.sis.dto.AIChatResponseDto;
import com.app.sis.service.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AIChatController {
    
    private final AIChatService aiChatService;
    
    /**
     * Send a message to AI assistant
     */
    @PostMapping
    public ResponseEntity<AIChatResponseDto> sendMessage(@RequestBody AIChatRequestDto request) {
        log.info("Received AI chat message from user {}: {}", request.getUserId(), request.getMessage());
        
        try {
            AIChatResponseDto response = aiChatService.processMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing AI chat message", e);
            return ResponseEntity.ok(new AIChatResponseDto(
                "Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau.",
                "error",
                false
            ));
        }
    }
    
    /**
     * Get chat history for a user
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<AIChatMessageDto>> getChatHistory(@PathVariable Long userId) {
        log.info("Fetching chat history for user {}", userId);
        List<AIChatMessageDto> history = aiChatService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Clear chat history for a user
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<Void> clearChatHistory(@PathVariable Long userId) {
        log.info("Clearing chat history for user {}", userId);
        aiChatService.clearChatHistory(userId);
        return ResponseEntity.ok().build();
    }
}
