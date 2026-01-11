package com.example.springboot_api.dto.websocket;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message gửi cho Task Owner - chi tiết progress của AI task.
 * Topic: /topic/ai-task/{aiSetId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskProgressMessage {

    /**
     * AI Set ID
     */
    private UUID aiSetId;

    /**
     * Loại message: progress, done, failed
     */
    private String type;

    /**
     * Step hiện tại: queued, summarizing, generating, saving
     */
    private String step;

    /**
     * % hoàn thành (0-100)
     */
    private int progress;

    /**
     * Message mô tả trạng thái
     */
    private String message;

    /**
     * Data bổ sung khi done (optional)
     */
    private Map<String, Object> data;

    /**
     * Loại AI set: quiz, flashcards, video, etc.
     */
    private String setType;

    // Static factory methods
    public static AiTaskProgressMessage queued(UUID aiSetId, String setType) {
        return AiTaskProgressMessage.builder()
                .aiSetId(aiSetId)
                .type("progress")
                .step("queued")
                .progress(0)
                .message("Đang xếp hàng chờ xử lý...")
                .setType(setType)
                .build();
    }

    public static AiTaskProgressMessage progress(UUID aiSetId, String step, int progress, String message) {
        return AiTaskProgressMessage.builder()
                .aiSetId(aiSetId)
                .type("progress")
                .step(step)
                .progress(progress)
                .message(message)
                .build();
    }

    public static AiTaskProgressMessage done(UUID aiSetId, String setType, Map<String, Object> data) {
        return AiTaskProgressMessage.builder()
                .aiSetId(aiSetId)
                .type("done")
                .step("completed")
                .progress(100)
                .message("Hoàn thành!")
                .setType(setType)
                .data(data)
                .build();
    }

    public static AiTaskProgressMessage failed(UUID aiSetId, String errorMessage) {
        return AiTaskProgressMessage.builder()
                .aiSetId(aiSetId)
                .type("failed")
                .step("error")
                .progress(0)
                .message(errorMessage)
                .build();
    }
}
