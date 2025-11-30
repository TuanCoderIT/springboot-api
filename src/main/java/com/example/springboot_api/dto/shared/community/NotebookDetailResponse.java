package com.example.springboot_api.dto.shared.community;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record NotebookDetailResponse(
        UUID id,
        String title,
        String description,
        String type,
        String visibility,
        String thumbnailUrl,
        UUID createdById,
        String createdByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        MemberInfo members,
        FileInfo files
) {
    public record MemberInfo(
            Long totalCount,
            List<MemberItem> items
    ) {}

    public record MemberItem(
            UUID userId,
            String userFullName,
            String userEmail,
            String role,
            String status,
            OffsetDateTime joinedAt
    ) {}

    public record FileInfo(
            Long totalCount,
            List<FileItem> items
    ) {}

    public record FileItem(
            UUID id,
            String originalFilename,
            String mimeType,
            Long fileSize,
            String storageUrl,
            String status,
            OffsetDateTime createdAt
    ) {}
}

