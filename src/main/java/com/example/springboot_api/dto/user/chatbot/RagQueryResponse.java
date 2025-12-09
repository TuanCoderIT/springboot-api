package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RagQueryResponse(
        UUID id,
        String question,
        String answer,
        Map<String, Object> sourceChunks, // JSONB chứa: file_id, file_name, file_type, chunk_index, metadata, score,
                                          // bounding_box, ocr_text
        Integer latencyMs,
        OffsetDateTime createdAt) {
}
