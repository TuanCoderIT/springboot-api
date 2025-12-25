package com.example.springboot_api.dto.user.regulation;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * User-facing response cho regulation file.
 */
@Data
@Builder
public class UserRegulationFileResponse {
    private UUID id;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String storageUrl;
    private Integer pagesCount;
    private String uploadedByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
