package com.example.springboot_api.dto.user.flashcard;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho một flashcard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardResponse {

    private UUID id;
    private String frontText;
    private String backText;
    private String hint;
    private String example;
    private String imageUrl;
    private String audioUrl;
    private Map<String, Object> extraMetadata;
    private OffsetDateTime createdAt;
}

