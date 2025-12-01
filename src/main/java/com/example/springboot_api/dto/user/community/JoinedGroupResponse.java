package com.example.springboot_api.dto.user.community;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JoinedGroupResponse(
        UUID id,
        String title,
        String description,
        String visibility,
        String thumbnailUrl,
        Long memberCount,
        String membershipStatus,
        String role,
        OffsetDateTime joinedAt,
        OffsetDateTime createdAt
) {}

