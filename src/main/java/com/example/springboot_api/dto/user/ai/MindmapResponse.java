package com.example.springboot_api.dto.user.ai;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Response cho Mindmap.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapResponse {

    private UUID id;
    private UUID notebookId;
    private String title;

    /**
     * Cấu trúc mindmap dạng cây:
     * {
     * "root": {
     * "id": "string",
     * "title": "string",
     * "summary": "string",
     * "children": [...]
     * }
     * }
     */
    private Map<String, Object> mindmap;

    /**
     * Layout info (optional, for rendering)
     */
    private Map<String, Object> layout;

    private UUID aiSetId;
    private UUID createdById;
    private String createdByName;
    private String createdByAvatar;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
