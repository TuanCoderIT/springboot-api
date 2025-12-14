package com.example.springboot_api.mappers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.chatbot.AiSetResponse;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi AI Set entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class AiSetMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert NotebookAiSet entity sang AiSetResponse DTO.
     */
    public AiSetResponse toAiSetResponse(NotebookAiSet set, boolean isOwner) {
        if (set == null) {
            return null;
        }

        String userFullName = null;
        String userAvatar = null;
        UUID userId = null;

        if (set.getCreatedBy() != null) {
            userId = set.getCreatedBy().getId();
            userFullName = set.getCreatedBy().getFullName();
            userAvatar = urlNormalizer.normalizeToFull(set.getCreatedBy().getAvatarUrl());
        }

        int fileCount = set.getNotebookAiSetFiles() != null ? set.getNotebookAiSetFiles().size() : 0;

        // Prepare outputStats với normalized audioUrl
        Map<String, Object> outputStats = null;
        if (set.getOutputStats() != null && !set.getOutputStats().isEmpty()) {
            outputStats = new HashMap<>(set.getOutputStats());
            // Normalize audioUrl nếu có
            Object audioUrlRaw = outputStats.get("audioUrl");
            if (audioUrlRaw instanceof String audioUrl) {
                outputStats.put("audioUrl", urlNormalizer.normalizeToFull(audioUrl));
            }
        }

        return AiSetResponse.builder()
                .id(set.getId())
                .notebookId(set.getNotebook() != null ? set.getNotebook().getId() : null)
                .userId(userId)
                .userFullName(userFullName)
                .userAvatar(userAvatar)
                .setType(set.getSetType())
                .status(set.getStatus())
                .errorMessage(set.getErrorMessage())
                .title(set.getTitle())
                .description(set.getDescription())
                .createdAt(set.getCreatedAt())
                .startedAt(set.getStartedAt())
                .finishedAt(set.getFinishedAt())
                .updatedAt(set.getUpdatedAt())
                .fileCount(fileCount)
                .isOwner(isOwner)
                .outputStats(outputStats)
                .build();
    }
}
