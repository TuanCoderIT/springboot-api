package com.example.springboot_api.dto.admin.regulation;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Response cho file quy chế.
 */
@Data
@Builder
public class RegulationFileResponse {
    private UUID id;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String status;
    private Boolean ocrDone;
    private Boolean embeddingDone;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Instant createdAt;
    private Instant updatedAt;
}
