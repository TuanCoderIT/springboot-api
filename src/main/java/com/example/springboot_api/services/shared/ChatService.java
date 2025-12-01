package com.example.springboot_api.services.shared;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import com.example.springboot_api.models.MessageReaction;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.NotebookMessage;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.shared.MessageReactionRepository;
import com.example.springboot_api.repositories.shared.NotebookMessageRepository;
import com.example.springboot_api.repositories.shared.AuthRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final NotebookMessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final NotebookMemberRepository memberRepository;
    private final NotebookRepository notebookRepository;
    private final AuthRepository userRepository;

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
        // Reload to get reactions
        return toMessageDTO(message);
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
            userInfo = new UserInfoDTO(
                    message.getUser().getId(),
                    message.getUser().getFullName(),
                    message.getUser().getEmail(),
                    message.getUser().getAvatarUrl()
            );
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
                message.getCreatedAt()
        );
    }

    private ReactionDTO toReactionDTO(MessageReaction reaction) {
        UserInfoDTO userInfo = new UserInfoDTO(
                reaction.getUser().getId(),
                reaction.getUser().getFullName(),
                reaction.getUser().getEmail(),
                reaction.getUser().getAvatarUrl()
        );

        return new ReactionDTO(
                reaction.getId(),
                reaction.getEmoji(),
                userInfo,
                reaction.getCreatedAt()
        );
    }
}

