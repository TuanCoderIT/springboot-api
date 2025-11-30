package com.example.springboot_api.dto.admin.notebook;

import java.util.UUID;

public record ApproveRejectBlockRequest(
        UUID notebookId,
        UUID userId,
        String action // "approve", "reject", "block"
) {}

