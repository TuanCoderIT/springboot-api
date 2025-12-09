package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho một conversation item trong danh sách.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationItem {
    private UUID id;
    private String title;
    private UUID notebookId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String firstMessage; // Tin nhắn đầu tiên (content của message đầu tiên)
    private Long totalMessages; // Tổng số tin nhắn trong conversation
}
