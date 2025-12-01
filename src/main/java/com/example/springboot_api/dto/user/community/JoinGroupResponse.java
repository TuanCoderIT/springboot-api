package com.example.springboot_api.dto.user.community;

import java.util.UUID;

public record JoinGroupResponse(
        UUID notebookId,
        String status,
        String message
) {}

