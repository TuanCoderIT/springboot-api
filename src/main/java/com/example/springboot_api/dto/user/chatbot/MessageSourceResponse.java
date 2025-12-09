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
public class MessageSourceResponse {
    private UUID id;
    private String sourceType; // "RAG" | "WEB"
    private UUID fileId;
    private Integer chunkIndex;
    private String title;
    private String url;
    private String snippet;
    private String provider;
    private Integer webIndex;
    private Double score;
}

