package com.example.springboot_api.dto.user.quiz;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho danh sách quiz theo NotebookAiSet.
 * Bao gồm thông tin AI Set và danh sách tất cả quiz kèm options.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizListResponse {

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

    // Danh sách quiz
    private List<QuizResponse> quizzes;

    // Thống kê
    private int totalQuizzes;
}
