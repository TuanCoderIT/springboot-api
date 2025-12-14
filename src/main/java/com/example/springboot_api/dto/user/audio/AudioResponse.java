package com.example.springboot_api.dto.user.audio;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho thông tin audio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioResponse {

    private UUID id;
    private String audioUrl;
    private String language;
    private String voiceName;
    private Integer durationSeconds;
    private String textSource;
    private OffsetDateTime createdAt;
}
