package com.example.springboot_api.dto.user.flashcard;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho danh sách flashcards theo NotebookAiSet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardListResponse {

    // Thông tin NotebookAiSet
    private UUID aiSetId;
    private String title;
    private String description;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime finishedAt;

    // Thông tin user tạo
    private UUID createdById;
    private String createdByName;
    private String createdByAvatar;

    // Thông tin notebook
    private UUID notebookId;

    // Danh sách flashcards
    private List<FlashcardResponse> flashcards;

    // Thống kê
    private int totalFlashcards;
}

