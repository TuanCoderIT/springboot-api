package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho AI Task.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiTaskResponse {

    private UUID id;
    private UUID notebookId;
    private UUID userId;
    private String userFullName;
    private String userAvatar;
    private String taskType;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Số lượng files được sử dụng để tạo task.
     */
    private Integer fileCount;

    /**
     * Flag xác định task này có phải của user hiện tại không.
     */
    private Boolean isOwner;
}
