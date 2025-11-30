package com.example.springboot_api.dto.shared.community;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AvailableGroupResponse(
        UUID id,
        String title,
        String description,
        String visibility,
        String thumbnailUrl,
        Long memberCount,
        OffsetDateTime createdAt
) {}

