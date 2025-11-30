package com.example.springboot_api.dto.shared.community;

import java.util.UUID;

public record JoinGroupResponse(
        UUID notebookId,
        String status,
        String message
) {}

