package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho NotebookAiSet.
 * Dùng để trả về thông tin AI Set cho frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSetResponse {

    private UUID id;
    private UUID notebookId;
    private UUID userId;
    private String userFullName;
    private String userAvatar;
    private String setType; // "quiz", "flashcard", "tts", "video", etc.
    private String status; // "queued", "processing", "done", "failed"
    private String errorMessage;
    private String title;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime updatedAt;
    private int fileCount;
    private boolean isOwner;
}
