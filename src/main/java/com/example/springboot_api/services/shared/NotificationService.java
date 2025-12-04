package com.example.springboot_api.services.shared;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.dto.shared.notification.CreateNotificationRequest;
import com.example.springboot_api.dto.shared.notification.NotificationResponse;
import com.example.springboot_api.dto.shared.notification.ShareNotificationRequest;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.Notification;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.AuthRepository;
import com.example.springboot_api.repositories.shared.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthRepository userRepository;
    private final UserRepository adminUserRepository;
    private final NotebookMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi notification cho user cụ thể theo ID
     */
    @Transactional
    public NotificationResponse sendToUser(UUID userId, CreateNotificationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = createNotification(user, request);
        notification = notificationRepository.save(notification);

        NotificationResponse response = toResponse(notification);

        // Gửi notification qua WebSocket
        sendNotificationViaWebSocket(userId, response);

        return response;
    }

    /**
     * Gửi notification cho tất cả members của một notebook/group
     */
    @Transactional
    public List<NotificationResponse> sendToGroup(UUID notebookId, CreateNotificationRequest request) {
        return sendToGroup(notebookId, request, null);
    }

    /**
     * Gửi notification cho tất cả members của một notebook/group (có thể exclude
     * một số users)
     */
    @Transactional
    public List<NotificationResponse> sendToGroup(UUID notebookId, CreateNotificationRequest request,
            List<UUID> excludeUserIds) {
        // Lấy tất cả members đã approved
        List<NotebookMember> members = memberRepository.findApprovedMembers(notebookId);

        if (members.isEmpty()) {
            log.warn("No approved members found for notebook: {}", notebookId);
            return new ArrayList<>();
        }

        // Filter out excluded users
        if (excludeUserIds != null && !excludeUserIds.isEmpty()) {
            members = members.stream()
                    .filter(member -> !excludeUserIds.contains(member.getUser().getId()))
                    .collect(Collectors.toList());
        }

        if (members.isEmpty()) {
            log.debug("No members to notify after excluding users for notebook: {}", notebookId);
            return new ArrayList<>();
        }

        List<Notification> notifications = new ArrayList<>();
        for (NotebookMember member : members) {
            Notification notification = createNotification(member.getUser(), request);
            notifications.add(notification);
        }

        notifications = notificationRepository.saveAll(notifications);

        List<NotificationResponse> responses = notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Gửi notification qua WebSocket cho từng member
        for (NotificationResponse response : responses) {
            sendNotificationViaWebSocket(response.getUserId(), response);
        }

        return responses;
    }

    /**
     * Gửi notification cho tất cả admin
     */
    @Transactional
    public List<NotificationResponse> sendToAdmins(CreateNotificationRequest request) {
        // Lấy tất cả users có role ADMIN
        List<User> admins = adminUserRepository.allUserPage(null, "ADMIN",
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        if (admins.isEmpty()) {
            log.warn("No admin users found");
            return new ArrayList<>();
        }

        List<Notification> notifications = new ArrayList<>();
        for (User admin : admins) {
            Notification notification = createNotification(admin, request);
            notifications.add(notification);
        }

        notifications = notificationRepository.saveAll(notifications);

        List<NotificationResponse> responses = notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Gửi notification qua WebSocket cho từng admin
        for (NotificationResponse response : responses) {
            sendNotificationViaWebSocket(response.getUserId(), response);
        }

        return responses;
    }

    /**
     * Share notification với các users, groups hoặc admins
     */
    @Transactional
    public List<NotificationResponse> shareNotification(ShareNotificationRequest request) {
        // Lấy notification gốc
        Notification originalNotification = notificationRepository.findById(request.getNotificationId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        List<Notification> notifications = new ArrayList<>();

        // Share với các users cụ thể
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            for (UUID userId : request.getUserIds()) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                Notification sharedNotification = createSharedNotification(user, originalNotification);
                notifications.add(sharedNotification);
            }
        }

        // Share với các groups
        if (request.getNotebookIds() != null && !request.getNotebookIds().isEmpty()) {
            for (UUID notebookId : request.getNotebookIds()) {
                List<NotebookMember> members = memberRepository.findApprovedMembers(notebookId);
                for (NotebookMember member : members) {
                    Notification sharedNotification = createSharedNotification(
                            member.getUser(), originalNotification);
                    notifications.add(sharedNotification);
                }
            }
        }

        // Share với admins
        if (Boolean.TRUE.equals(request.getShareToAdmins())) {
            List<User> admins = adminUserRepository.allUserPage(null, "ADMIN",
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();
            for (User admin : admins) {
                Notification sharedNotification = createSharedNotification(admin, originalNotification);
                notifications.add(sharedNotification);
            }
        }

        if (notifications.isEmpty()) {
            log.warn("No targets specified for sharing notification");
            return new ArrayList<>();
        }

        notifications = notificationRepository.saveAll(notifications);

        List<NotificationResponse> responses = notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Gửi notification qua WebSocket cho từng user được share
        for (NotificationResponse response : responses) {
            sendNotificationViaWebSocket(response.getUserId(), response);
        }

        return responses;
    }

    /**
     * Gửi notification qua WebSocket đến user cụ thể
     */
    private void sendNotificationViaWebSocket(UUID userId, NotificationResponse notification) {
        try {
            String destination = "/topic/user/" + userId + "/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Sent notification via WebSocket to user: {} at destination: {}", userId, destination);

            // Gửi số lượng notification chưa đọc
            sendUnreadCountViaWebSocket(userId);
        } catch (Exception e) {
            log.error("Error sending notification via WebSocket to user {}: {}", userId, e.getMessage(), e);
            // Không throw exception để không ảnh hưởng đến việc lưu notification
        }
    }

    /**
     * Gửi số lượng notification chưa đọc qua WebSocket
     */
    private void sendUnreadCountViaWebSocket(UUID userId) {
        try {
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
            String destination = "/topic/user/" + userId + "/notifications/count";
            messagingTemplate.convertAndSend(destination, unreadCount);
            log.debug("Sent unread count via WebSocket to user: {} - count: {}", userId, unreadCount);
        } catch (Exception e) {
            log.error("Error sending unread count via WebSocket to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Tạo notification mới từ request
     */
    private Notification createNotification(User user, CreateNotificationRequest request) {
        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> metadata = request.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        return Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .metadata(metadata)
                .action(request.getAction())
                .roleTarget(request.getRoleTarget())
                .isRead(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Tạo shared notification từ notification gốc
     */
    private Notification createSharedNotification(User user, Notification original) {
        OffsetDateTime now = OffsetDateTime.now();

        // Tạo metadata mới với thông tin về notification gốc
        Map<String, Object> metadata = new HashMap<>(original.getMetadata());
        metadata.put("sharedFrom", original.getId().toString());
        metadata.put("originalUserId", original.getUser().getId().toString());

        return Notification.builder()
                .user(user)
                .type(original.getType())
                .title(original.getTitle())
                .content(original.getContent())
                .url(original.getUrl())
                .metadata(metadata)
                .action(original.getAction())
                .roleTarget(original.getRoleTarget())
                .isRead(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Convert Notification entity to Response DTO
     */
    private NotificationResponse toResponse(Notification notification) {
        User user = notification.getUser();

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
