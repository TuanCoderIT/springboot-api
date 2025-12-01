package com.example.springboot_api.controllers.shared;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.shared.chat.MessageDTO;
import com.example.springboot_api.dto.shared.chat.ReactRequest;
import com.example.springboot_api.dto.shared.chat.ReactionUpdateDTO;
import com.example.springboot_api.dto.shared.chat.SendMessageRequest;
import com.example.springboot_api.dto.shared.chat.TypingNotificationDTO;
import com.example.springboot_api.dto.shared.chat.TypingRequest;
import com.example.springboot_api.dto.shared.chat.UserInfoDTO;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.AuthRepository;
import com.example.springboot_api.services.shared.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/notebooks/{notebookId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthRepository userRepository;

    @MessageMapping("/notebooks/{notebookId}/chat.send")
    public void sendMessage(
            @DestinationVariable UUID notebookId,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        
        try {
            MessageDTO message = chatService.sendMessage(notebookId, userId, request);
            
            // Broadcast to all subscribers
            messagingTemplate.convertAndSend("/topic/notebooks/" + notebookId + "/chat", message);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/notebooks/{notebookId}/chat.react")
    public void reactToMessage(
            @DestinationVariable UUID notebookId,
            @Payload ReactRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        
        try {
            ReactionUpdateDTO update = chatService.toggleReaction(
                    notebookId, userId, request.getMessageId(), request.getEmoji());
            
            // Broadcast reaction update
            messagingTemplate.convertAndSend(
                    "/topic/notebooks/" + notebookId + "/chat", update);
        } catch (Exception e) {
            log.error("Error reacting to message: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/notebooks/{notebookId}/chat.typing")
    public void handleTyping(
            @DestinationVariable UUID notebookId,
            @Payload TypingRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserInfoDTO userInfo = new UserInfoDTO(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getAvatarUrl()
            );
            
            TypingNotificationDTO notification = new TypingNotificationDTO(
                    userId, userInfo, request.isTyping());
            
            // Broadcast typing indicator (exclude sender)
            messagingTemplate.convertAndSend(
                    "/topic/notebooks/" + notebookId + "/chat", notification);
        } catch (Exception e) {
            log.error("Error handling typing: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<MessageDTO>> getMessageHistory(
            @PathVariable UUID notebookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            List<MessageDTO> messages = chatService.getMessageHistory(
                    notebookId, user.getId(), page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error fetching message history: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }
}

