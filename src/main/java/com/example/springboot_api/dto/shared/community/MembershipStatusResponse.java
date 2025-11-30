package com.example.springboot_api.dto.shared.community;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipStatusResponse(
        UUID notebookId,
        boolean isMember,
        boolean canJoin,
        String status,
        String role,
        OffsetDateTime joinedAt,
        OffsetDateTime requestedAt) {
}
