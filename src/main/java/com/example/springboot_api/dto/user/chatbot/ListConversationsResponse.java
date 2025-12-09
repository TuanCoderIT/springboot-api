package com.example.springboot_api.dto.user.chatbot;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho danh sách conversations với cursor pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListConversationsResponse {
    private List<ConversationItem> conversations;
    private String cursorNext; // UUID của conversation cũ nhất trong response
    private boolean hasMore; // Còn conversation cũ hơn không
}
