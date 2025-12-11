package com.example.springboot_api.dto.user.notebook;

import java.util.List;

/**
 * Response DTO cho danh sách thành viên với cursor-based pagination.
 */
public record NotebookMembersResponse(
        List<NotebookMemberItem> items,
        String cursorNext,
        boolean hasMore,
        long total) {
}
