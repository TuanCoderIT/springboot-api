package com.example.springboot_api.dto.user.chatbot;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho danh sách messages với cursor pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListMessagesResponse {
    private List<ChatResponse> messages;
    private String cursorNext; // UUID của message cũ nhất trong response
    private boolean hasMore; // Còn message cũ hơn không
}

