package com.example.springboot_api.dto.user.timeline;

import java.util.List;
import java.util.UUID;

import lombok.Data;

/**
 * Request DTO cho generate timeline.
 */
@Data
public class TimelineGenerateRequest {

    /**
     * Danh sách file IDs để tạo timeline.
     */
    private List<UUID> fileIds;

    /**
     * Mode tạo timeline:
     * - "time": Ưu tiên mốc thời gian
     * - "logic": Theo tiến trình logic/học tập
     */
    private String mode = "logic";

    /**
     * Số events tối đa (default 25).
     */
    private Integer maxEvents = 25;

    /**
     * Ngôn ngữ output (default "vi").
     */
    private String language = "vi";

    /**
     * Yêu cầu bổ sung (optional).
     */
    private String additionalRequirements;
}
