package com.example.springboot_api.dto.lecturer.chapter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterItemResponse {
    private UUID id;
    private String itemType;
    private UUID refId;
    private String title;
    private Integer sortOrder;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private Boolean visibleInLesson;
    private Boolean visibleInNotebook;
}
