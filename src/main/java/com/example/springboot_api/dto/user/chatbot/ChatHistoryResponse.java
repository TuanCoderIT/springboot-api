package com.example.springboot_api.dto.user.chatbot;

import java.util.List;

public record ChatHistoryResponse(
        List<RagQueryResponse> messages,
        String cursorNext, // UUID của message cũ nhất trong response, dùng để lấy tiếp các message cũ hơn
        boolean hasMore) { // Còn message cũ hơn không
}
