package com.example.springboot_api.dto.user.tts;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;

@Builder
public record TtsVoiceResponse(
        UUID id,
        String voiceId,
        String voiceName,
        String description,
        String provider,
        String gender,
        String language,
        String accent,
        String style,
        String ageGroup,
        String useCase,
        String sampleAudioUrl,
        String sampleText,
        Integer sampleDurationMs,
        Double defaultSpeed,
        Double defaultPitch,
        Boolean isActive,
        Boolean isPremium,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
