package com.example.springboot_api.dto.user.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO cho từng thành viên trong danh sách members của notebook.
 */
public record NotebookMemberItem(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String avatarUrl,
        String role,
        String status,
        OffsetDateTime joinedAt) {
}
