package com.example.springboot_api.controllers.shared;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.shared.notification.CreateNotificationRequest;
import com.example.springboot_api.dto.shared.notification.NotificationResponse;
import com.example.springboot_api.dto.shared.notification.ShareNotificationRequest;
import com.example.springboot_api.repositories.shared.AuthRepository;
import com.example.springboot_api.repositories.shared.NotificationRepository;
import com.example.springboot_api.services.shared.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final AuthRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi notification cho user cụ thể
     * POST /api/notifications/send/user/{userId}
     */
    @PostMapping("/send/user/{userId}")
    public ResponseEntity<NotificationResponse> sendToUser(
            @PathVariable UUID userId,
            @RequestBody CreateNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            NotificationResponse response = notificationService.sendToUser(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending notification to user: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Gửi notification cho tất cả members của một notebook/group
     * POST /api/notifications/send/group/{notebookId}
     */
    @PostMapping("/send/group/{notebookId}")
    public ResponseEntity<List<NotificationResponse>> sendToGroup(
            @PathVariable UUID notebookId,
            @RequestBody CreateNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<NotificationResponse> responses = notificationService.sendToGroup(notebookId, request);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error sending notification to group: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Gửi notification cho tất cả admin
     * POST /api/notifications/send/admins
     */
    @PostMapping("/send/admins")
    public ResponseEntity<List<NotificationResponse>> sendToAdmins(
            @RequestBody CreateNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // Chỉ admin mới được gửi notification cho admin khác
        com.example.springboot_api.models.User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<NotificationResponse> responses = notificationService.sendToAdmins(request);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error sending notification to admins: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Share notification với users, groups hoặc admins
     * POST /api/notifications/share
     */
    @PostMapping("/share")
    public ResponseEntity<List<NotificationResponse>> shareNotification(
            @RequestBody ShareNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<NotificationResponse> responses = notificationService.shareNotification(request);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error sharing notification: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Lấy danh sách notifications của user hiện tại
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<com.example.springboot_api.models.Notification> notifications = notificationRepository
                    .findByUserIdWithFilters(
                            user.getId(), isRead, type, pageable);

            List<NotificationResponse> content = notifications.getContent().stream()
                    .map(this::toResponse)
                    .toList();

            PagedResponse<NotificationResponse> response = new PagedResponse<>(
                    content,
                    new PagedResponse.Meta(
                            notifications.getNumber(),
                            notifications.getSize(),
                            notifications.getTotalElements(),
                            notifications.getTotalPages()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Đánh dấu notification là đã đọc
     * PATCH /api/notifications/{notificationId}/read
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            com.example.springboot_api.models.Notification notification = notificationRepository
                    .findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // Kiểm tra user có quyền đọc notification này không
            if (!notification.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }

            notification.setIsRead(true);
            notification.setReadAt(java.time.OffsetDateTime.now());
            notification.setUpdatedAt(java.time.OffsetDateTime.now());
            notification = notificationRepository.save(notification);

            NotificationResponse response = toResponse(notification);

            // Gửi update qua WebSocket
            try {
                String destination = "/topic/user/" + user.getId() + "/notifications";
                messagingTemplate.convertAndSend(destination, response);

                // Gửi số lượng chưa đọc
                long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
                String countDestination = "/topic/user/" + user.getId() + "/notifications/count";
                messagingTemplate.convertAndSend(countDestination, unreadCount);
            } catch (Exception e) {
                log.error("Error sending notification update via WebSocket: {}", e.getMessage(), e);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Đánh dấu tất cả notifications của user hiện tại là đã đọc
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
            int updatedCount = notificationRepository.markAllAsReadByUserId(
                    user.getId(), now, now);

            // Gửi số lượng chưa đọc qua WebSocket (sẽ là 0 sau khi mark all)
            try {
                long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
                String countDestination = "/topic/user/" + user.getId() + "/notifications/count";
                messagingTemplate.convertAndSend(countDestination, unreadCount);
            } catch (Exception e) {
                log.error("Error sending unread count update via WebSocket: {}", e.getMessage(), e);
            }

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("updatedCount", updatedCount);
            response.put("unreadCount", 0L);
            response.put("message", "All notifications marked as read");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Lấy số lượng notifications chưa đọc
     * GET /api/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting unread count: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }

    private NotificationResponse toResponse(com.example.springboot_api.models.Notification notification) {
        com.example.springboot_api.models.User user = notification.getUser();

        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .url(notification.getUrl())
                .metadata(notification.getMetadata())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .action(notification.getAction())
                .roleTarget(notification.getRoleTarget())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
