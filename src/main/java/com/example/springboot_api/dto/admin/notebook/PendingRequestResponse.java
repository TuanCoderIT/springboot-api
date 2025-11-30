package com.example.springboot_api.dto.admin.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PendingRequestResponse(
        UUID id,
        UUID notebookId,
        String notebookTitle,
        UUID userId,
        String userFullName,
        String userEmail,
        String status,
        OffsetDateTime createdAt
) {}

