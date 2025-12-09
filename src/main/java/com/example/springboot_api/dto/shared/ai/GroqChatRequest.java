package com.example.springboot_api.dto.shared.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for Groq Chat API.
 */
public record GroqChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") Integer maxTokens,
        Double temperature) {

    public record Message(
            String role,
            String content) {
    }

    public static GroqChatRequest create(String prompt) {
        return new GroqChatRequest(
                "llama-3.3-70b-versatile",
                List.of(new Message("user", prompt)),
                1000,
                0.7);
    }
}
