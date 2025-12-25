package com.example.springboot_api.dto.admin.regulation;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Response cho regulation notebook với statistics.
 */
@Data
@Builder
public class RegulationNotebookResponse {
    private UUID id;
    private String title;
    private String description;
    private String type;
    private String visibility;
    private UUID createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    // Statistics
    private Long totalFiles;
    private Long pendingFiles;
    private Long approvedFiles;
    private Long processingFiles;
    private Long failedFiles;
    private Long ocrDoneFiles;
    private Long embeddingDoneFiles;
}
