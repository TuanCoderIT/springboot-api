package com.example.springboot_api.mappers;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.audio.AudioListResponse;
import com.example.springboot_api.dto.user.audio.AudioResponse;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.TtsAsset;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Audio entities sang DTOs.
 * Tách riêng logic mapping khỏi Service để code clean hơn.
 */
@Component
@RequiredArgsConstructor
public class AudioMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert TtsAsset entity sang AudioResponse DTO.
     */
    public AudioResponse toAudioResponse(TtsAsset asset) {
        if (asset == null) {
            return null;
        }

        return AudioResponse.builder()
                .id(asset.getId())
                .audioUrl(urlNormalizer.normalizeToFull(asset.getAudioUrl()))
                .language(asset.getLanguage())
                .voiceName(asset.getVoiceName())
                .durationSeconds(asset.getDurationSeconds())
                .textSource(asset.getTextSource())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    /**
     * Convert list TtsAsset entities sang list AudioResponse DTOs.
     */
    public List<AudioResponse> toAudioResponseList(List<TtsAsset> assets) {
        if (assets == null) {
            return List.of();
        }

        return assets.stream()
                .map(this::toAudioResponse)
                .toList();
    }

    /**
     * Build AudioListResponse từ NotebookAiSet và danh sách AudioResponse.
     */
    public AudioListResponse toAudioListResponse(NotebookAiSet aiSet, List<AudioResponse> audios) {
        if (aiSet == null) {
            return null;
        }

        UUID createdById = null;
        String createdByName = null;
        String createdByAvatar = null;

        if (aiSet.getCreatedBy() != null) {
            createdById = aiSet.getCreatedBy().getId();
            createdByName = aiSet.getCreatedBy().getFullName();
            createdByAvatar = urlNormalizer.normalizeToFull(aiSet.getCreatedBy().getAvatarUrl());
        }

        UUID notebookId = aiSet.getNotebook() != null ? aiSet.getNotebook().getId() : null;

        // Lấy audio chính (audio đầu tiên)
        AudioResponse primaryAudio = audios.isEmpty() ? null : audios.get(0);

        return AudioListResponse.builder()
                .aiSetId(aiSet.getId())
                .title(aiSet.getTitle())
                .description(aiSet.getDescription())
                .status(aiSet.getStatus())
                .errorMessage(aiSet.getErrorMessage())
                .createdAt(aiSet.getCreatedAt())
                .finishedAt(aiSet.getFinishedAt())
                .createdById(createdById)
                .createdByName(createdByName)
                .createdByAvatar(createdByAvatar)
                .notebookId(notebookId)
                .audios(audios)
                .primaryAudio(primaryAudio)
                .totalAudios(audios.size())
                .build();
    }
}
