package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho tạo conversation mới.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationResponse {
    private UUID id;
    private String title;
    private UUID notebookId;
    private OffsetDateTime createdAt;
}

