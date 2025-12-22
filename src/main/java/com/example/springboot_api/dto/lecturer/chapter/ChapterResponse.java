package com.example.springboot_api.dto.lecturer.chapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** Danh sách items trong chương (có thể null nếu không fetch) */
    private List<ChapterItemResponse> items;
}
