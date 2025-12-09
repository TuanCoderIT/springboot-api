package com.example.springboot_api.dto.user.chatbot;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSourceResponse {
    private String sourceType; // "RAG"
    private UUID fileId;
    private Integer chunkIndex;
    private Double score;
    private String provider; // "rag"
    private String content; // Nội dung chunk
    private Double similarity;
    private Double distance;
}

