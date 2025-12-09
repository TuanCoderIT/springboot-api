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
public class ModelResponse {
    private UUID id;
    private String code;
    private String provider; // "openai" | "anthropic" | "gemini" | "local"
}

