package com.example.springboot_api.mappers;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.flashcard.FlashcardListResponse;
import com.example.springboot_api.dto.user.flashcard.FlashcardResponse;
import com.example.springboot_api.models.Flashcard;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Flashcard entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class FlashcardMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert Flashcard entity sang FlashcardResponse DTO.
     */
    public FlashcardResponse toFlashcardResponse(Flashcard card) {
        if (card == null) {
            return null;
        }

        return FlashcardResponse.builder()
                .id(card.getId())
                .frontText(card.getFrontText())
                .backText(card.getBackText())
                .hint(card.getHint())
                .example(card.getExample())
                .imageUrl(card.getImageUrl())
                .audioUrl(card.getAudioUrl())
                .extraMetadata(card.getExtraMetadata())
                .createdAt(card.getCreatedAt())
                .build();
    }

    /**
     * Convert list Flashcard entities sang list FlashcardResponse DTOs.
     */
    public List<FlashcardResponse> toFlashcardResponseList(List<Flashcard> cards) {
        if (cards == null) {
            return List.of();
        }

        return cards.stream()
                .map(this::toFlashcardResponse)
                .toList();
    }

    /**
     * Build FlashcardListResponse từ NotebookAiSet và danh sách FlashcardResponse.
     */
    public FlashcardListResponse toFlashcardListResponse(NotebookAiSet aiSet, List<FlashcardResponse> cards) {
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

        return FlashcardListResponse.builder()
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
                .flashcards(cards)
                .totalFlashcards(cards.size())
                .build();
    }
}
