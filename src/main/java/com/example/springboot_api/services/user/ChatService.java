package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.dto.shared.chat.MessageDTO;
import com.example.springboot_api.dto.shared.chat.ReactionDTO;
import com.example.springboot_api.dto.shared.chat.ReactionUpdateDTO;
import com.example.springboot_api.dto.shared.chat.SendMessageRequest;
import com.example.springboot_api.dto.shared.chat.UserInfoDTO;
import com.example.springboot_api.dto.shared.notification.CreateNotificationRequest;
import com.example.springboot_api.models.MessageReaction;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.NotebookMessage;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.AuthRepository;
import com.example.springboot_api.repositories.shared.NotebookMessageRepository;
import com.example.springboot_api.repositories.user.MessageReactionRepository;
import com.example.springboot_api.services.shared.NotificationService;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

        private final NotebookMessageRepository messageRepository;
        private final MessageReactionRepository reactionRepository;
        private final NotebookMemberRepository memberRepository;
        private final NotebookRepository notebookRepository;
        private final AuthRepository userRepository;
        private final NotificationService notificationService;
        private final UrlNormalizer urlNormalizer;

        @Transactional
        public MessageDTO sendMessage(UUID notebookId, UUID userId, SendMessageRequest request) {
                // Validate membership
                NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                                .orElseThrow(() -> new RuntimeException("User is not a member of this notebook"));

                if (!"approved".equals(member.getStatus())) {
                        throw new RuntimeException("User membership is not approved");
                }

                Notebook notebook = notebookRepository.findById(notebookId)
                                .orElseThrow(() -> new RuntimeException("Notebook not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                NotebookMessage message = NotebookMessage.builder()
                                .notebook(notebook)
                                .user(user)
                                .type("user")
                                .content(request.getContent())
                                .createdAt(OffsetDateTime.now())
                                .build();

                if (request.getReplyToMessageId() != null) {
                        NotebookMessage replyTo = messageRepository.findById(request.getReplyToMessageId())
                                        .orElseThrow(() -> new RuntimeException("Reply message not found"));
                        message.setReplyToMessage(replyTo);
                }

                message = messageRepository.save(message);

                // Gửi notification cho tất cả members (trừ người gửi)
                sendChatNotification(notebook, user, message);

                // Reload to get reactions
                return toMessageDTO(message);
        }

        /**
         * Gửi notification khi có tin nhắn mới trong nhóm chat
         */
        private void sendChatNotification(Notebook notebook, User sender, NotebookMessage message) {
                try {
                        // Tạo notification request
                        String notificationTitle = "Tin nhắn mới trong " + notebook.getTitle();
                        String notificationContent = sender.getFullName() + ": " +
                                        (message.getContent().length() > 100
                                                        ? message.getContent().substring(0, 100) + "..."
                                                        : message.getContent());

                        // Tạo metadata với thông tin message
                        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                        metadata.put("notebookId", notebook.getId().toString());
                        metadata.put("notebookTitle", notebook.getTitle());
                        metadata.put("messageId", message.getId().toString());
                        metadata.put("senderId", sender.getId().toString());
                        metadata.put("senderName", sender.getFullName());
                        metadata.put("messageContent", message.getContent());
                        metadata.put("messageCreatedAt", message.getCreatedAt().toString());

                        CreateNotificationRequest notificationRequest = CreateNotificationRequest.builder()
                                        .type("chat_message")
                                        .title(notificationTitle)
                                        .content(notificationContent)
                                        .url("/notebooks/" + notebook.getId() + "/chat")
                                        .metadata(metadata)
                                        .action("view_chat")
                                        .build();

                        // Sử dụng sendToGroup với exclude sender
                        List<UUID> excludeUserIds = java.util.List.of(sender.getId());
                        notificationService.sendToGroup(notebook.getId(), notificationRequest, excludeUserIds);

                        log.debug("Sent chat notifications to group: {} (excluding sender: {})",
                                        notebook.getId(), sender.getId());
                } catch (Exception e) {
                        log.error("Error sending chat notifications: {}", e.getMessage(), e);
                        // Không throw exception để không ảnh hưởng đến việc gửi message
                }
        }

        @Transactional
        public ReactionUpdateDTO toggleReaction(UUID notebookId, UUID userId, UUID messageId, String emoji) {
                // Validate membership
                NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                                .orElseThrow(() -> new RuntimeException("User is not a member of this notebook"));

                if (!"approved".equals(member.getStatus())) {
                        throw new RuntimeException("User membership is not approved");
                }

                NotebookMessage message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new RuntimeException("Message not found"));

                if (!message.getNotebook().getId().equals(notebookId)) {
                        throw new RuntimeException("Message does not belong to this notebook");
                }

                Optional<MessageReaction> existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(
                                messageId, userId, emoji);

                if (existing.isPresent()) {
                        // Remove reaction
                        reactionRepository.delete(existing.get());
                        return new ReactionUpdateDTO(messageId, null, "removed");
                } else {
                        // Add reaction
                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        MessageReaction reaction = MessageReaction.builder()
                                        .message(message)
                                        .user(user)
                                        .emoji(emoji)
                                        .createdAt(OffsetDateTime.now())
                                        .build();

                        reaction = reactionRepository.save(reaction);
                        ReactionDTO reactionDTO = toReactionDTO(reaction);
                        return new ReactionUpdateDTO(messageId, reactionDTO, "added");
                }
        }

        public List<MessageDTO> getMessageHistory(UUID notebookId, UUID userId, int page, int size) {
                // Validate membership
                NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                                .orElseThrow(() -> new RuntimeException("User is not a member of this notebook"));

                if (!"approved".equals(member.getStatus())) {
                        throw new RuntimeException("User membership is not approved");
                }

                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                List<NotebookMessage> messages = messageRepository.findRecentByNotebookId(notebookId, pageable);

                return messages.stream()
                                .map(this::toMessageDTO)
                                .collect(Collectors.toList());
        }

        private MessageDTO toMessageDTO(NotebookMessage message) {
                UserInfoDTO userInfo = null;
                if (message.getUser() != null) {
                        String normalizedAvatarUrl = urlNormalizer.normalizeToFull(message.getUser().getAvatarUrl());
                        userInfo = new UserInfoDTO(
                                        message.getUser().getId(),
                                        message.getUser().getFullName(),
                                        message.getUser().getEmail(),
                                        normalizedAvatarUrl);
                }

                // Load reactions separately to avoid lazy loading issues
                List<MessageReaction> reactionsList = reactionRepository.findByMessageIdWithUser(message.getId());
                List<ReactionDTO> reactions = reactionsList.stream()
                                .map(this::toReactionDTO)
                                .collect(Collectors.toList());

                UUID replyToId = message.getReplyToMessage() != null
                                ? message.getReplyToMessage().getId()
                                : null;

                return new MessageDTO(
                                message.getId(),
                                userInfo,
                                message.getContent(),
                                replyToId,
                                reactions,
                                message.getCreatedAt());
        }

        private ReactionDTO toReactionDTO(MessageReaction reaction) {
                String normalizedAvatarUrl = urlNormalizer.normalizeToFull(reaction.getUser().getAvatarUrl());
                UserInfoDTO userInfo = new UserInfoDTO(
                                reaction.getUser().getId(),
                                reaction.getUser().getFullName(),
                                reaction.getUser().getEmail(),
                                normalizedAvatarUrl);

                return new ReactionDTO(
                                reaction.getId(),
                                reaction.getEmoji(),
                                userInfo,
                                reaction.getCreatedAt());
        }
}
