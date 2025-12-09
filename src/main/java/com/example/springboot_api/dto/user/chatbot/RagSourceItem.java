package com.example.springboot_api.dto.user.chatbot;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RagSourceItem {
    private UUID fileId;
    private int chunkIndex;
    private double similarity;
    private String content; // snippet của chunk
}
