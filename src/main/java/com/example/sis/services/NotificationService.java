package com.example.sis.services;

import com.example.sis.dtos.NotificationDTO;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Notification;
import com.example.sis.models.User;
import com.example.sis.repositories.NotificationRepository;
import com.example.sis.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EntityManager entityManager;
    private final UserRepository userRepository;
    
    public NotificationService(NotificationRepository notificationRepository,
                              SimpMessagingTemplate messagingTemplate,
                              EntityManager entityManager,
                              UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }
    
    /**
     * Tạo và gửi thông báo (dùng cho admin/manager tạo thủ công)
     */
    @Transactional
    public NotificationDTO createAndSend(NotificationDTO dto) {
        Notification notification = new Notification();
        User user = entityManager.getReference(User.class, dto.getUserId());
        notification.setUser(user);
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setRelatedType(dto.getRelatedType());
        notification.setRelatedId(dto.getRelatedId());
        notification.setSeverity(dto.getSeverity() != null ? dto.getSeverity() : "low");
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // Convert to DTO
        NotificationDTO result = convertToDTO(saved);
        
        // Send via WebSocket to specific user
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + dto.getUserId(),
            result
        );
        
        return result;
    }
    
    /**
     * Tạo thông báo nhanh (dùng trong service layer)
     */
    @Transactional
    public void createAndSend(Integer userId, String type, String title, String message) {
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(userId);
        dto.setType(type);
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setSeverity("low");
        createAndSend(dto);
    }
    
    /**
     * Tạo thông báo với thông tin đầy đủ (dùng trong service layer)
     */
    @Transactional
    public void createAndSend(Integer userId, String type, String title, String message, 
                             String relatedType, Long relatedId, String severity) {
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(userId);
        dto.setType(type);
        dto.setTitle(title);
        dto.setMessage(message);
        dto.setRelatedType(relatedType);
        dto.setRelatedId(relatedId);
        dto.setSeverity(severity);
        createAndSend(dto);
    }
    
    /**
     * Lấy danh sách thông báo của user
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getByUserId(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy danh sách thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadByUserId(Integer userId) {
        List<Notification> notifications = notificationRepository.findUnreadByUserId(userId);
        return notifications.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Đếm số thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(Integer userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    /**
     * Đánh dấu một thông báo là đã đọc
     */
    @Transactional
    public void markAsRead(Long notificationId, Integer userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new NotFoundException("Notification not found or not owned by user");
        }
    }
    
    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }
    
    /**
     * Xóa thông báo
     */
    @Transactional
    public void delete(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new NotFoundException("Notification not found"));
        
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own notifications");
        }
        
        notificationRepository.delete(notification);
    }
    
    /**
     * Gửi thông báo broadcast đến nhiều user hoặc tất cả user
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @param recipientIds Danh sách userId nhận thông báo (nếu null/empty thì gửi cho tất cả)
     * @param severity Mức độ: INFO, WARNING, ERROR
     * @return Số lượng thông báo đã gửi
     */
    @Transactional
    public int broadcastNotification(String title, String message, List<Integer> recipientIds, String severity) {
        System.out.println("Broadcasting notification - Title: " + title + ", Recipients: " + recipientIds);
        
        List<User> recipients;
        
        // Nếu recipientIds null hoặc empty thì lấy tất cả user
        if (recipientIds == null || recipientIds.isEmpty()) {
            System.out.println("Fetching all users for broadcast...");
            recipients = userRepository.findAll();
            System.out.println("Found " + recipients.size() + " users");
        } else {
            System.out.println("Fetching specific users: " + recipientIds);
            recipients = userRepository.findAllById(recipientIds);
            System.out.println("Found " + recipients.size() + " users");
        }
        
        if (recipients.isEmpty()) {
            System.out.println("WARNING: No recipients found!");
            return 0;
        }
        
        // Tạo thông báo cho từng user
        for (User user : recipients) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("SYSTEM_ANNOUNCEMENT"); // Type mới cho thông báo hệ thống
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setSeverity(severity != null ? severity : "INFO");
            notification.setCreatedAt(LocalDateTime.now());
            notification.setIsRead(false);
            
            Notification saved = notificationRepository.save(notification);
            
            // Gửi real-time qua WebSocket nếu user đang online
            try {
                NotificationDTO dto = convertToDTO(saved);
                messagingTemplate.convertAndSend("/topic/notifications/" + user.getUserId(), dto);
            } catch (Exception e) {
                // Log error nhưng không throw để tiếp tục gửi cho user khác
                System.err.println("Failed to send WebSocket notification to user " + user.getUserId() + ": " + e.getMessage());
            }
        }
        
        return recipients.size();
    }
    
    /**
     * Gửi thông báo cho admin/manager của center (BAO GỒM cả người thực hiện - activity log)
     * @param centerId ID của center
     * @param excludeUserId User ID không nhận thông báo (set null để gửi cho tất cả)
     * @param type Loại thông báo
     * @param title Tiêu đề
     * @param message Nội dung
     * @param relatedType Loại đối tượng liên quan
     * @param relatedId ID đối tượng liên quan
     * @param severity Mức độ
     */
    @Transactional
    public void notifyAdminsExcept(Integer centerId, Integer excludeUserId, String type, 
                                    String title, String message, String relatedType, 
                                    Long relatedId, String severity) {
        // Tìm tất cả admin/manager của center (SUPER_ADMIN + CENTER_MANAGER + ACADEMIC_STAFF)
        List<User> admins = userRepository.findAdminsByCenterId(centerId);
        
        // KHÔNG lọc bỏ người thực hiện - để admin thấy activity log của chính mình
        // admins = admins.stream()
        //         .filter(admin -> !admin.getUserId().equals(excludeUserId))
        //         .collect(Collectors.toList());
        
        // Gửi thông báo cho từng admin (bao gồm cả người thực hiện)
        for (User admin : admins) {
            try {
                createAndSend(
                    admin.getUserId(),
                    type,
                    title,
                    message,
                    relatedType,
                    relatedId,
                    severity
                );
            } catch (Exception e) {
                // Log error nhưng không throw để tiếp tục gửi cho admin khác
                System.err.println("Failed to send notification to admin " + admin.getUserId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Convert entity to DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUser().getUserId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRelatedType(notification.getRelatedType());
        dto.setRelatedId(notification.getRelatedId());
        dto.setSeverity(notification.getSeverity());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
