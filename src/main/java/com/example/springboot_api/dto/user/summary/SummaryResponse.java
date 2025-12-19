package com.example.springboot_api.dto.user.summary;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho Summary với TTS audio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {

    private UUID id;
    private String title;
    private String contentMd;
    private String scriptTts;
    private String language;
    private String audioUrl;
    private String audioFormat;
    private Integer audioDurationMs;
    private String ttsProvider;
    private String ttsModel;
    private String voiceId;
    private String voiceLabel;
    private Float voiceSpeed;
    private Float voicePitch;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private boolean hasAudio;
}
