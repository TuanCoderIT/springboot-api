package com.example.springboot_api.dto.admin.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID notebookId,
        String notebookTitle,
        UUID userId,
        String userFullName,
        String userEmail,
        String userAvatarUrl,
        String role,
        String status,
        OffsetDateTime joinedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UserStatistics statistics) {
    
    public record UserStatistics(
            Long fileCount,
            Long videoCount,
            Long flashcardCount,
            Long ttsCount,
            Long quizCount,
            Long messageCount,
            Long ragQueryCount) {}
}
