package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    // ID của message
    private UUID id;

    // Nội dung trả lời (markdown)
    private String content;

    // Mode thực tế sử dụng: RAG | WEB | HYBRID | LLM_ONLY | AUTO
    private String mode;

    // Role: "user" | "assistant"
    private String role;

    // Context (JSONB)
    private Map<String, Object> context;

    // Thời gian tạo
    private OffsetDateTime createdAt;

    // Metadata (JSONB)
    private Map<String, Object> metadata;

    // Thông tin model
    private ModelResponse model;

    // Sources (RAG và WEB gộp chung)
    private List<SourceResponse> sources;

    // Files (nếu user upload ảnh)
    private List<FileResponse> files;
}
