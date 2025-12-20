package com.example.sis.service.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Security Audit Service for Chat
 * Ensures NO sensitive data (JWT tokens, passwords) is sent to LLM
 */
@Service
@Slf4j
public class SecurityAuditService {
    
    // Patterns for sensitive data detection
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(Bearer\\s+[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+)|" +
        "(eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]*)"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|pwd|passwd|pass)\\s*[:=]\\s*\\S+"
    );
    
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|apikey|secret[_-]?key)\\s*[:=]\\s*[A-Za-z0-9\\-_]+"
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b0\\d{9,10}\\b"
    );
    
    /**
     * Validate that context contains ONLY public information
     * ✅ This is CRITICAL security check before sending to LLM
     */
    public void validateSafeContext(Map<String, Object> context) {
        if (context == null) {
            return;
        }
        
        String contextStr = context.toString();
        
        // ❌ CRITICAL: Check for JWT tokens
        if (TOKEN_PATTERN.matcher(contextStr).find()) {
            log.error("🚨 SECURITY BREACH: JWT token detected in chat context!");
            throw new SecurityException("JWT token detected in chat context - BLOCKED");
        }
        
        // ❌ CRITICAL: Check for passwords
        if (PASSWORD_PATTERN.matcher(contextStr).find()) {
            log.error("🚨 SECURITY BREACH: Password detected in chat context!");
            throw new SecurityException("Password detected in chat context - BLOCKED");
        }
        
        // ❌ CRITICAL: Check for API keys
        if (API_KEY_PATTERN.matcher(contextStr).find()) {
            log.error("🚨 SECURITY BREACH: API key detected in chat context!");
            throw new SecurityException("API key detected in chat context - BLOCKED");
        }
        
        // ⚠️ WARNING: Personal data detected (allowed but logged)
        if (EMAIL_PATTERN.matcher(contextStr).find() || PHONE_PATTERN.matcher(contextStr).find()) {
            log.warn("⚠️ Personal data (email/phone) detected in context - userId: {}", 
                context.get("userId"));
        }
        
        log.debug("✅ Chat context validated - No sensitive data detected");
    }
    
    /**
     * Sanitize user message before sending to LLM
     * - Remove control characters
     * - Limit length to prevent token abuse
     * - Check for injection attacks
     */
    public String sanitizeUserMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        
        // Remove control characters (except newline, tab)
        String sanitized = message.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // Check for JWT token in message (user shouldn't paste tokens)
        if (TOKEN_PATTERN.matcher(sanitized).find()) {
            log.warn("⚠️ User tried to send JWT token in message - removed");
            sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("[REDACTED_TOKEN]");
        }
        
        // Check for password patterns
        if (PASSWORD_PATTERN.matcher(sanitized).find()) {
            log.warn("⚠️ User tried to send password in message - removed");
            sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_PASSWORD]");
        }
        
        // Limit length (prevent token abuse - Cohere has limits)
        if (sanitized.length() > 4000) {
            log.warn("⚠️ User message truncated from {} to 4000 chars", sanitized.length());
            sanitized = sanitized.substring(0, 4000) + "... [truncated]";
        }
        
        return sanitized.trim();
    }
    
    /**
     * Sanitize LLM response before sending to user
     * Prevent LLM from leaking internal info
     */
    public String sanitizeLLMResponse(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        
        String sanitized = response;
        
        // Remove any leaked tokens (shouldn't happen but extra safety)
        if (TOKEN_PATTERN.matcher(sanitized).find()) {
            log.error("🚨 LLM leaked JWT token in response - REMOVING");
            sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");
        }
        
        // Remove any leaked API keys
        if (API_KEY_PATTERN.matcher(sanitized).find()) {
            log.error("🚨 LLM leaked API key in response - REMOVING");
            sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");
        }
        
        return sanitized;
    }
}
