package com.example.sis.controllers;

import com.example.sis.dtos.NotificationDTO;
import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }
    
    /**
     * Tạo và gửi thông báo mới (Admin/Manager only)
     * POST /api/notifications
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'CENTER_MANAGER')")
    public ResponseEntity<NotificationDTO> createNotification(@Valid @RequestBody NotificationDTO dto) {
        NotificationDTO created = notificationService.createAndSend(dto);
        return ResponseEntity.ok(created);
    }
    
    /**
     * Lấy danh sách thông báo của user HIỆN TẠI (từ token)
     * GET /api/notifications
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(Authentication auth) {
        Integer currentUserId = getCurrentUserId(auth);
        List<NotificationDTO> notifications = notificationService.getByUserId(currentUserId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy danh sách thông báo chưa đọc của user hiện tại
     * GET /api/notifications/unread
     */
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication auth) {
        Integer currentUserId = getCurrentUserId(auth);
        List<NotificationDTO> notifications = notificationService.getUnreadByUserId(currentUserId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Đếm số thông báo chưa đọc của user hiện tại
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(Authentication auth) {
        Integer currentUserId = getCurrentUserId(auth);
        int count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * Đánh dấu một thông báo là đã đọc
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        Integer currentUserId = getCurrentUserId(auth);
        notificationService.markAsRead(id, currentUserId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Đánh dấu tất cả thông báo của user hiện tại là đã đọc
     * PATCH /api/notifications/mark-all-read
     */
    @PatchMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        Integer currentUserId = getCurrentUserId(auth);
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Xóa thông báo của user hiện tại
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication auth) {
        Integer currentUserId = getCurrentUserId(auth);
        notificationService.delete(id, currentUserId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Gửi thông báo broadcast đến nhiều user hoặc tất cả user
     * POST /api/notifications/broadcast
     * Body: {
     *   "title": "Tiêu đề",
     *   "message": "Nội dung",
     *   "recipientIds": [1, 2, 3] (nếu null hoặc empty thì gửi cho tất cả user),
     *   "severity": "INFO"
     * }
     */
    @PostMapping("/broadcast")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'CENTER_MANAGER')")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @Valid @RequestBody com.example.sis.dtos.notification.BroadcastNotificationRequest request) {
        int sentCount = notificationService.broadcastNotification(
                request.getTitle(),
                request.getMessage(),
                request.getRecipientIds(),
                request.getSeverity()
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sentCount", sentCount,
                "message", "Đã gửi thông báo thành công"
        ));
    }
    
    /**
     * Extract userId from JWT token
     * Looks up user_id from database using email claim in JWT.
     */
    private Integer getCurrentUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            // Get email from JWT token
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Email claim not found in JWT token");
            }
            
            // Look up user_id from database using email
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            
            return user.getUserId();
        }
        throw new RuntimeException("Authentication principal is not a JWT");
    }
}
