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
public class SourceResponse {
    // Common fields
    private String sourceType; // "RAG" | "WEB"
    private Double score;
    private String provider;

    // RAG fields (null nếu sourceType = "WEB")
    private UUID fileId;
    private String fileName; // Tên file gốc
    private Integer chunkIndex;
    private String content; // Nội dung chunk
    private Double similarity;
    private Double distance;

    // WEB fields (null nếu sourceType = "RAG")
    private Integer webIndex;
    private String url;
    private String title;
    private String snippet;
    private String imageUrl;
    private String favicon;
}
