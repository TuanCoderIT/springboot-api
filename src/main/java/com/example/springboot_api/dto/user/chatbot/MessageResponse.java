package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private UUID id;
    private String role; // "user" | "assistant"
    private String content;
    private String mode; // "RAG" | "WEB" | "HYBRID" | "LLM_ONLY"
    private UUID modelId;
    private String modelCode;
    private OffsetDateTime createdAt;
    private List<MessageSourceResponse> sources;
    private List<MessageFileResponse> files;
}
