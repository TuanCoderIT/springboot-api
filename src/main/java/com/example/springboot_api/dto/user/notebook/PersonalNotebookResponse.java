package com.example.springboot_api.dto.user.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PersonalNotebookResponse(
        UUID id,
        String title,
        String description,
        String type,
        String visibility,
        String thumbnailUrl,
        Long fileCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
