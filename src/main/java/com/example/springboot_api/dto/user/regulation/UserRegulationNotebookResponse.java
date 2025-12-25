package com.example.springboot_api.dto.user.regulation;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * User-facing response cho regulation notebook.
 */
@Data
@Builder
public class UserRegulationNotebookResponse {
    private UUID id;
    private String title;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long totalFiles; // Chỉ đếm file done/approved
}
