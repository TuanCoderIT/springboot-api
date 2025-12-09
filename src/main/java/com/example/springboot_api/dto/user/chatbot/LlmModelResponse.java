package com.example.springboot_api.dto.user.chatbot;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho LLM Model trong response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmModelResponse {
    private UUID id;
    private String code;
    private String provider;
    private String displayName;
    private Boolean isActive;
    private Boolean isDefault;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
}
