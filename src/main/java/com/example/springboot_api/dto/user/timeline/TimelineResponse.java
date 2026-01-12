package com.example.springboot_api.dto.user.timeline;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho toàn bộ timeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineResponse {

    /**
     * AI Set ID chứa timeline này.
     */
    private UUID aiSetId;

    /**
     * Tiêu đề timeline (do LLM sinh).
     */
    private String title;

    /**
     * Mode đã dùng: time hoặc logic.
     */
    private String mode;

    /**
     * Số events trong timeline.
     */
    private int totalEvents;

    /**
     * Trạng thái: queued, processing, done, failed.
     */
    private String status;

    /**
     * Thời gian tạo.
     */
    private OffsetDateTime createdAt;

    /**
     * Danh sách events.
     */
    private List<TimelineEventResponse> events;

    /**
     * Thông tin người tạo.
     */
    private CreatorInfo createdBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private UUID id;
        private String fullName;
        private String avatarUrl;
    }
}
