package com.example.springboot_api.dto.shared.community;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityPreviewResponse(
        UUID id,
        String title,
        String description,
        String visibility,
        String thumbnailUrl,
        UUID createdById,
        String createdByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Statistics statistics,
        List<RecentMessagePreview> recentMessages,
        List<FilePreview> recentFiles
) {
    public record Statistics(
            Long memberCount,
            Long fileCount,
            Long messageCount,
            Long flashcardCount,
            Long quizCount
    ) {}

    public record RecentMessagePreview(
            UUID id,
            String type,
            String contentPreview,
            String authorName,
            OffsetDateTime createdAt
    ) {}

    public record FilePreview(
            UUID id,
            String originalFilename,
            String mimeType,
            Long fileSize,
            OffsetDateTime createdAt
    ) {}
}

