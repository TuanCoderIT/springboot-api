package com.example.springboot_api.dto.user.chatbot;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ChatRequest {
    private UUID conversationId;

    private String message;
    private UUID modelId; // đổi lại camelCase cho đẹp
    private ChatMode mode; // RAG | WEB | HYBRID | LLM_ONLY | AUTO

    private List<UUID> ragFileIds; // dùng cho RAG
}
