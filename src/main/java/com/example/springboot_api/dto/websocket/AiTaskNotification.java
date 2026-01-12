package com.example.springboot_api.dto.websocket;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification gửi cho tất cả Notebook Members khi AI task được tạo/hoàn
 * thành/xóa.
 * Topic: /topic/notebook/{notebookId}/ai-tasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskNotification {

    /**
     * AI Set ID
     */
    private UUID aiSetId;

    /**
     * Notebook ID chứa task
     */
    private UUID notebookId;

    /**
     * Loại notification: created, done, deleted
     */
    private String type;

    /**
     * Loại AI set: quiz, flashcards, video, audio, summary, mindmap, suggestions
     */
    private String setType;

    /**
     * Tiêu đề task
     */
    private String title;

    /**
     * Trạng thái: queued, processing, done, failed
     */
    private String status;

    /**
     * Thông tin người tạo
     */
    private CreatorInfo createdBy;

    /**
     * Thời gian
     */
    private OffsetDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private UUID id;
        private String fullName;
        private String avatarUrl;
    }

    // Static factory methods
    public static AiTaskNotification created(UUID aiSetId, UUID notebookId, String setType,
            String title, CreatorInfo creator) {
        return AiTaskNotification.builder()
                .aiSetId(aiSetId)
                .notebookId(notebookId)
                .type("created")
                .setType(setType)
                .title(title)
                .status("queued")
                .createdBy(creator)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static AiTaskNotification done(UUID aiSetId, UUID notebookId, String setType,
            String title, CreatorInfo creator) {
        return AiTaskNotification.builder()
                .aiSetId(aiSetId)
                .notebookId(notebookId)
                .type("done")
                .setType(setType)
                .title(title)
                .status("done")
                .createdBy(creator)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static AiTaskNotification deleted(UUID aiSetId, UUID notebookId, String setType,
            String title, CreatorInfo deleter) {
        return AiTaskNotification.builder()
                .aiSetId(aiSetId)
                .notebookId(notebookId)
                .type("deleted")
                .setType(setType)
                .title(title)
                .status("deleted")
                .createdBy(deleter)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
