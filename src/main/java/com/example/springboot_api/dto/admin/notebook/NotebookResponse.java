package com.example.springboot_api.dto.admin.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotebookResponse(
        UUID id,
        String title,
        String description,
        String type,
        String visibility,
        String thumbnailUrl,
        Long memberCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
